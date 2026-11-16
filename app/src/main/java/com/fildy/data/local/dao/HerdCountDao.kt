package com.fildy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.fildy.data.local.entity.HerdCountEntity
import com.fildy.data.model.CattleCategory

@Dao
interface HerdCountDao {
    /** Current head count, 0 if there's no row. */
    @Query("SELECT COALESCE((SELECT headCount FROM herd_count WHERE paddockId = :paddockId AND category = :category), 0)")
    suspend fun getHeadCount(paddockId: Long, category: CattleCategory): Int

    @Upsert
    suspend fun upsert(count: HerdCountEntity)

    @Query("DELETE FROM herd_count WHERE paddockId = :paddockId AND category = :category")
    suspend fun delete(paddockId: Long, category: CattleCategory)

    @Insert
    suspend fun insertAll(counts: List<HerdCountEntity>)

    /** Sets the count, removing the row when it reaches zero. */
    suspend fun setHeadCount(paddockId: Long, category: CattleCategory, headCount: Int) {
        if (headCount == 0) delete(paddockId, category) else upsert(HerdCountEntity(paddockId, category, headCount))
    }
}
