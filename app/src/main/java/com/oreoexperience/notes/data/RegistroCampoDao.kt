package com.oreoexperience.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroCampoDao {

    @Query("SELECT * FROM registros_campo ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<RegistroCampo>>

    @Query(
        """
        SELECT * FROM registros_campo
        WHERE dateMillis >= :startMillis AND dateMillis < :endMillis
        ORDER BY dateMillis ASC
        """
    )
    fun observeRange(startMillis: Long, endMillis: Long): Flow<List<RegistroCampo>>

    @Query("SELECT * FROM registros_campo WHERE dateMillis = :dayMillis LIMIT 1")
    suspend fun getByDay(dayMillis: Long): RegistroCampo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(r: RegistroCampo): Long

    @Update
    suspend fun update(r: RegistroCampo)

    @Delete
    suspend fun delete(r: RegistroCampo)

    @Query("DELETE FROM registros_campo")
    suspend fun deleteAll()
}
