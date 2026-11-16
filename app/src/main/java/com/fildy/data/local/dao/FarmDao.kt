package com.fildy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.fildy.data.local.entity.FarmEntity
import com.fildy.data.local.relation.FarmWithPaddocks
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    /** The app manages a single farm. */
    @Query("SELECT * FROM farm ORDER BY id LIMIT 1")
    suspend fun getFarm(): FarmEntity?

    /**
     * The farm with its paddocks and head counts. Re-emits whenever `farm`, `paddock` or
     * `herd_count` change; @Transaction loads the whole tree as one consistent snapshot.
     */
    @Transaction
    @Query("SELECT * FROM farm ORDER BY id LIMIT 1")
    fun observeFarmWithPaddocks(): Flow<FarmWithPaddocks?>

    @Query("SELECT COUNT(*) FROM farm")
    suspend fun count(): Int

    @Insert
    suspend fun insert(farm: FarmEntity)
}
