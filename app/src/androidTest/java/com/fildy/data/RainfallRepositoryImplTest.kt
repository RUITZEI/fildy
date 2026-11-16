package com.fildy.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.local.FildyDatabase
import com.fildy.data.local.entity.RainfallEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RainfallRepositoryImplTest {

    private lateinit var database: FildyDatabase

    private val today = LocalDate.of(2026, 11, 16)
    private val lastNight = LocalDate.of(2026, 11, 15) // seeded with 18 mm

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun last60Days_containsTheSeed_newestFirst() = runTest {
        val rain = rainfallRepository(database).observeRecent().first()

        assertEquals(8, rain.size)
        assertEquals(131.0, rain.sumOf { it.millimeters }, 0.0)
        assertEquals(lastNight, rain.first().date)
        assertEquals(18.0, rain.first().millimeters, 0.0)
    }

    @Test
    fun windowExcludesOlderRain() = runTest {
        // 2026-11-20: the window starts on 2026-09-22, so the 2026-09-21 record drops out.
        val later = Clock.offset(testClock, Duration.ofDays(4))
        assertEquals(7, rainfallRepository(database, later).observeRecent().first().size)
    }

    @Test
    fun setRainfall_newDay_addsARecord() = runTest {
        val repository = rainfallRepository(database)
        val update = repository.setRainfall(today, 12.5)

        assertNull(update.previous)
        val rain = repository.observeRecent().first()
        assertEquals(9, rain.size)
        assertEquals(update.record, rain.first())
    }

    @Test
    fun setRainfall_existingDay_replacesIt() = runTest {
        val repository = rainfallRepository(database)
        val update = repository.setRainfall(lastNight, 20.0)

        assertEquals(18.0, update.previous!!.millimeters, 0.0)
        assertEquals(update.previous.id, update.record.id) // same row, updated in place
        val rain = repository.observeRecent().first()
        assertEquals(8, rain.size) // still one record for that day
        assertEquals(133.0, rain.sumOf { it.millimeters }, 0.0) // 131 - 18 + 20
    }

    @Test
    fun undo_replacement_restoresThePreviousRecord() = runTest {
        val repository = rainfallRepository(database)
        val original = repository.observeRecent().first().first { it.date == lastNight }

        val update = repository.setRainfall(lastNight, 20.0)
        repository.undo(update)

        assertEquals(original, repository.observeRecent().first().first { it.date == lastNight })
    }

    @Test
    fun undo_newDay_removesTheRecord() = runTest {
        val repository = rainfallRepository(database)
        repository.undo(repository.setRainfall(today, 12.5))

        assertEquals(8, repository.observeRecent().first().size)
    }

    @Test
    fun undo_afterTheDayChangedAgain_fails() = runTest {
        val repository = rainfallRepository(database)
        val first = repository.setRainfall(lastNight, 20.0)
        repository.setRainfall(lastNight, 25.0)

        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.undo(first) } }
        assertEquals(25.0, repository.observeRecent().first().first { it.date == lastNight }.millimeters, 0.0)
    }

    @Test
    fun database_rejectsTwoRecordsForTheSameDay() = runTest {
        val duplicate = RainfallEntity(farmId = 1, date = lastNight, millimeters = 5.0, recordedAt = Instant.EPOCH)
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { database.rainfallDao().insert(duplicate) } }
    }

    @Test
    fun deleteRainfall_removesTheRecord() = runTest {
        val repository = rainfallRepository(database)
        val record = repository.observeRecent().first().first()
        repository.deleteRainfall(record.id)

        assertEquals(7, repository.observeRecent().first().size)
    }
}
