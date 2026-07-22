package com.oreoexperience.notes.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Formato de respaldo histórico (v1). Sólo guarda `discursos` (no
 * incluye categoría ni Servicio del Campo). Se sigue aceptando al
 * importar para no romper backups viejos.
 */
@Serializable
data class BackupV1(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val discursos: List<Discurso> = emptyList(),
)

/**
 * Formato de respaldo v2. Incluye registros de Servicio del Campo y
 * las nuevas categorías. Es el formato que se exporta a partir de la
 * versión actual de la app.
 */
@Serializable
data class BackupV2(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val discursos: List<Discurso> = emptyList(),
    val registrosCampo: List<RegistroCampo> = emptyList(),
)

class BackupManager(
    private val context: Context,
    private val repository: DiscursoRepository,
    private val registroCampoRepository: RegistroCampoRepository,
    private val dao: DiscursoDao,
    private val registroCampoDao: RegistroCampoDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun export(uri: Uri): Result<Int> = runCatching {
        val notes = repository.observeAll().first()
        val campo = registroCampoRepository.observeAll().first()
        val payload = json.encodeToString(
            BackupV2(
                discursos = notes,
                registrosCampo = campo,
            )
        )
        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
            out.write(payload.toByteArray(Charsets.UTF_8))
        } ?: error("No se pudo abrir el destino para escribir")
        notes.size + campo.size
    }

    suspend fun import(uri: Uri, replaceAll: Boolean = false): Result<Int> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: error("No se pudo abrir el archivo")

        // Detectamos la versión del backup intentando primero como v2.
        val (discursos, registros) = try {
            val parsed = json.decodeFromString<BackupV2>(text)
            parsed.discursos to parsed.registrosCampo
        } catch (_: Throwable) {
            val legacy = json.decodeFromString<BackupV1>(text)
            legacy.discursos to emptyList<RegistroCampo>()
        }

        if (replaceAll) {
            dao.deleteAll()
            registroCampoDao.deleteAll()
        }
        discursos.forEach { d ->
            // Re-insertar como nuevo registro para evitar conflictos con IDs existentes.
            repository.upsert(d.copy(id = 0))
        }
        registros.forEach { r ->
            registroCampoRepository.upsert(r.copy(id = 0))
        }
        discursos.size + registros.size
    }
}
