package com.fildy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fildy.data.local.entity.RainfallEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface RainfallDao {
    /** Rain on or after [from], newest first. */
    @Query("SELECT * FROM rainfall WHERE date >= :from ORDER BY date DESC, id DESC")
    fun observeSince(from: LocalDate): Flow<List<RainfallEntity>>

    @Query("SELECT * FROM rainfall WHERE farmId = :farmId AND date = :date")
    suspend fun getByDate(farmId: Long, date: LocalDate): RainfallEntity?

    /** Records from [from] to [to] (both included), oldest first. */
    @Query("SELECT * FROM rainfall WHERE farmId = :farmId AND date BETWEEN :from AND :to ORDER BY date")
    suspend fun getBetween(farmId: Long, from: LocalDate, to: LocalDate): List<RainfallEntity>

    @Insert
    suspend fun insert(rainfall: RainfallEntity): Long

    @Update
    suspend fun update(rainfall: RainfallEntity)

    @Insert
    suspend fun insertAll(rainfall: List<RainfallEntity>)

    @Query("DELETE FROM rainfall WHERE id = :id")
    suspend fun delete(id: Long)
}
