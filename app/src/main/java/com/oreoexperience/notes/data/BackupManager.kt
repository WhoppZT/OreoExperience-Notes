package com.oreoexperience.notes.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupV1(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val discursos: List<Discurso> = emptyList(),
)

class BackupManager(
    private val context: Context,
    private val repository: DiscursoRepository,
    private val dao: DiscursoDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun export(uri: Uri): Result<Int> = runCatching {
        val snapshot = repository.observeAll().first()
        val payload = json.encodeToString(BackupV1(discursos = snapshot))
        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
            out.write(payload.toByteArray(Charsets.UTF_8))
        } ?: error("No se pudo abrir el destino para escribir")
        snapshot.size
    }

    suspend fun import(uri: Uri, replaceAll: Boolean = false): Result<Int> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: error("No se pudo abrir el archivo")
        val parsed = json.decodeFromString<BackupV1>(text)
        if (replaceAll) dao.deleteAll()
        parsed.discursos.forEach { d ->
            // Re-insertar como nuevo registro para evitar conflictos con IDs existentes.
            repository.upsert(d.copy(id = 0))
        }
        parsed.discursos.size
    }
}
