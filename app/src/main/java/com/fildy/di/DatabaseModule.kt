package com.fildy.di

import android.content.Context
import androidx.room.Room
import com.fildy.data.FarmTimeZone
import com.fildy.data.local.FildyDatabase
import com.fildy.data.local.RoomTransactionRunner
import com.fildy.data.local.TransactionRunner
import com.fildy.data.local.dao.FarmDao
import com.fildy.data.local.dao.HerdCountDao
import com.fildy.data.local.dao.PaddockDao
import com.fildy.data.local.dao.RainfallDao
import com.fildy.data.local.dao.StockChangeDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides the Room database, its DAOs and a [TransactionRunner]. Everything else depends on
 * the DAOs (and the runner for multi-DAO writes), never on [FildyDatabase] directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner

    companion object {
        // No fallbackToDestructiveMigration(): schema changes must ship a Migration, so user
        // data is never wiped silently.
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): FildyDatabase =
            Room.databaseBuilder(context, FildyDatabase::class.java, FildyDatabase.NAME).build()

        @Provides
        fun provideFarmDao(database: FildyDatabase): FarmDao = database.farmDao()

        @Provides
        fun providePaddockDao(database: FildyDatabase): PaddockDao = database.paddockDao()

        @Provides
        fun provideHerdCountDao(database: FildyDatabase): HerdCountDao = database.herdCountDao()

        @Provides
        fun provideRainfallDao(database: FildyDatabase): RainfallDao = database.rainfallDao()

        @Provides
        fun provideStockChangeDao(database: FildyDatabase): StockChangeDao = database.stockChangeDao()

        /**
         * Injectable clock so time-dependent logic (60-day window, timestamps) can be tested. Set
         * to the farm's time zone (Buenos Aires), whatever the phone is set to.
         */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.system(FarmTimeZone)
    }
}
