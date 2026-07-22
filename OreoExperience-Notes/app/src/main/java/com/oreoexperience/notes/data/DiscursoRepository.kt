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

    fun observeTrashed(): Flow<List<Discurso>> = dao.observeTrashed()

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

    /** Soft delete: marca la nota como eliminada (va a la papelera). */
    suspend fun trash(d: Discurso) {
        dao.update(d.copy(deletedAt = System.currentTimeMillis()))
    }

    /** Restaurar una nota desde la papelera. */
    suspend fun restore(d: Discurso) {
        dao.update(d.copy(deletedAt = null, updatedAt = System.currentTimeMillis()))
    }

    /** Cambiar el estado pin/unpin. */
    suspend fun setPinned(d: Discurso, pinned: Boolean) {
        dao.update(d.copy(pinned = pinned, updatedAt = System.currentTimeMillis()))
    }

    /** Hard delete real. */
    suspend fun delete(d: Discurso) = dao.delete(d)

    /** Purgar las notas en papelera más antiguas que [olderThan] ms. */
    suspend fun purgeOlderThan(olderThan: Long): List<Discurso> {
        val candidates = dao.trashedOlderThan(olderThan)
        candidates.forEach { dao.delete(it) }
        return candidates
    }

    suspend fun deleteAll() = dao.deleteAll()

    fun decodePoints(jsonStr: String): List<Punto> = try {
        json.decodeFromString(jsonStr.ifBlank { "[]" })
    } catch (_: Throwable) {
        emptyList()
    }

    fun encodePoints(list: List<Punto>): String = json.encodeToString(list)
}
