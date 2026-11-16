package com.fildy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.fildy.data.local.entity.PaddockEntity

@Dao
interface PaddockDao {
    @Insert
    suspend fun insertAll(paddocks: List<PaddockEntity>)
}
