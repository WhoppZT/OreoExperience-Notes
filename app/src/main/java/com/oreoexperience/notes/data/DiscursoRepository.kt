package com.oreoexperience.notes.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DiscursoRepository(private val dao: DiscursoDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    fun observeAll(): Flow<List<Discurso>> = dao.observeAll()

    fun observeById(id: Long): Flow<Discurso?> = dao.observeById(id)

    suspend fun get(id: Long): Discurso? = dao.getById(id)

    suspend fun upsert(d: Discurso): Long {
        val now = System.currentTimeMillis()
        return if (d.id == 0L) {
            dao.insert(d.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(d.copy(updatedAt = now))
            d.id
        }
    }

    suspend fun delete(d: Discurso) = dao.delete(d)

    suspend fun deleteAll() = dao.deleteAll()

    fun decodePoints(jsonStr: String): List<Punto> = try {
        json.decodeFromString(jsonStr.ifBlank { "[]" })
    } catch (_: Throwable) {
        emptyList()
    }

    fun encodePoints(list: List<Punto>): String = json.encodeToString(list)
}
