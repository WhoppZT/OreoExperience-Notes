package com.oreoexperience.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscursoDao {

    /** Notas activas (no en papelera). */
    @Query("SELECT * FROM discursos WHERE deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Discurso>>

    /** Notas en la papelera, ordenadas por fecha de borrado descendente. */
    @Query("SELECT * FROM discursos WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashed(): Flow<List<Discurso>>

    /** Notas en la papelera más antiguas que [olderThan] (ms). Para purga. */
    @Query("SELECT * FROM discursos WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun trashedOlderThan(olderThan: Long): List<Discurso>

    @Query("SELECT * FROM discursos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Discurso?

    @Query("SELECT * FROM discursos WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Discurso?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discurso: Discurso): Long

    @Update
    suspend fun update(discurso: Discurso)

    @Delete
    suspend fun delete(discurso: Discurso)

    @Query("DELETE FROM discursos")
    suspend fun deleteAll()
}
