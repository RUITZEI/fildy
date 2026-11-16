package com.fildy.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fildy.data.local.FildyDatabase
import com.fildy.data.local.RoomTransactionRunner
import com.fildy.data.repository.FarmRepositoryImpl
import com.fildy.data.repository.RainfallRepositoryImpl
import com.fildy.data.seed.DatabaseSeeder
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** 2026-11-16 at noon in Buenos Aires: the day the seed's rain data is relative to. */
val testClock: Clock = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

fun inMemoryDatabase(): FildyDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FildyDatabase::class.java).build()

// Wired by hand the same way Hilt does it: from the DAOs plus a transaction runner.

fun seeder(database: FildyDatabase) = DatabaseSeeder(
    context = ApplicationProvider.getApplicationContext(),
    farmDao = database.farmDao(),
    paddockDao = database.paddockDao(),
    herdCountDao = database.herdCountDao(),
    rainfallDao = database.rainfallDao(),
    transaction = RoomTransactionRunner(database),
    json = Json { ignoreUnknownKeys = true },
    clock = testClock,
)

fun farmRepository(database: FildyDatabase, clock: Clock = testClock) = FarmRepositoryImpl(
    farmDao = database.farmDao(),
    herdDao = database.herdCountDao(),
    changeDao = database.stockChangeDao(),
    transaction = RoomTransactionRunner(database),
    clock = clock,
)

fun rainfallRepository(database: FildyDatabase, clock: Clock = testClock) = RainfallRepositoryImpl(
    rainfallDao = database.rainfallDao(),
    farmDao = database.farmDao(),
    transaction = RoomTransactionRunner(database),
    clock = clock,
)
