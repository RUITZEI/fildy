package com.fildy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fildy.data.local.entity.StockChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockChangeDao {
    @Query("SELECT * FROM stock_change ORDER BY recordedAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<StockChangeEntity>>

    @Query("SELECT * FROM stock_change WHERE id = :id")
    suspend fun getById(id: Long): StockChangeEntity?

    @Insert
    suspend fun insert(change: StockChangeEntity): Long

    @Query("DELETE FROM stock_change WHERE id = :id")
    suspend fun delete(id: Long)
}
