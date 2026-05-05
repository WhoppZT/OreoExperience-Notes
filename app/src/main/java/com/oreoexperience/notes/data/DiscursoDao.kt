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

    @Query("SELECT * FROM discursos ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Discurso>>

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
