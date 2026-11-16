package com.fildy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fildy.data.local.dao.FarmDao
import com.fildy.data.local.dao.HerdCountDao
import com.fildy.data.local.dao.PaddockDao
import com.fildy.data.local.dao.RainfallDao
import com.fildy.data.local.dao.StockChangeDao
import com.fildy.data.local.entity.FarmEntity
import com.fildy.data.local.entity.HerdCountEntity
import com.fildy.data.local.entity.PaddockEntity
import com.fildy.data.local.entity.RainfallEntity
import com.fildy.data.local.entity.StockChangeEntity

@Database(
    entities = [
        FarmEntity::class,
        PaddockEntity::class,
        HerdCountEntity::class,
        RainfallEntity::class,
        StockChangeEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FildyDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao
    abstract fun paddockDao(): PaddockDao
    abstract fun herdCountDao(): HerdCountDao
    abstract fun rainfallDao(): RainfallDao
    abstract fun stockChangeDao(): StockChangeDao

    companion object {
        const val NAME = "fildy.db"
    }
}
