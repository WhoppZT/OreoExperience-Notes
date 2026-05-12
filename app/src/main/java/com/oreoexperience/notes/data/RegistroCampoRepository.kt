package com.oreoexperience.notes.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class RegistroCampoRepository(private val dao: RegistroCampoDao) {

    fun observeAll(): Flow<List<RegistroCampo>> = dao.observeAll()

    /**
     * Observa los registros de un mes específico (`year`, `monthZeroBased`
     * estilo [java.util.Calendar.MONTH]).
     */
    fun observeMonth(year: Int, monthZeroBased: Int): Flow<List<RegistroCampo>> {
        val start = Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, 1, 0, 0, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, 1, 0, 0, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return dao.observeRange(start, end)
    }

    suspend fun getByDay(dayMillis: Long): RegistroCampo? = dao.getByDay(dayMillis)

    /**
     * Inserta o actualiza un registro para el día indicado. Si ya existía
     * un registro para ese día (mismo `dateMillis`), se reemplaza.
     */
    suspend fun upsert(r: RegistroCampo): Long {
        val existing = dao.getByDay(r.dateMillis)
        val now = System.currentTimeMillis()
        return if (existing == null) {
            dao.insert(r.copy(createdAt = now, updatedAt = now))
        } else {
            val merged = r.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = now,
            )
            dao.update(merged)
            existing.id
        }
    }

    suspend fun delete(r: RegistroCampo) = dao.delete(r)

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        /** Normaliza un timestamp al inicio del día local (00:00). */
        fun startOfDayMillis(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
