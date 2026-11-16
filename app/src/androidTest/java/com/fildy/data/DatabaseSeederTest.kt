package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.local.FildyDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseSeederTest {

    private lateinit var database: FildyDatabase

    @Before
    fun setUp() {
        database = inMemoryDatabase()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun seedsTheBundledFile() = runTest {
        seeder(database).seedIfEmpty()

        val seeded = database.farmDao().observeFarmWithPaddocks().first()!!
        assertEquals("La Tranquera", seeded.farm.name)
        assertEquals(8, seeded.paddocks.size)
        assertEquals(14, seeded.paddocks.sumOf { it.herd.size })
        assertEquals(2247, seeded.paddocks.sumOf { paddock -> paddock.herd.sumOf { it.headCount } })
        val rain = database.rainfallDao().observeSince(LocalDate.of(2026, 1, 1)).first()
        assertEquals(8, rain.size)
        assertEquals(131.0, rain.sumOf { it.millimeters }, 0.0)
    }

    @Test
    fun seedsOnlyOnce() = runTest {
        val seeder = seeder(database)
        seeder.seedIfEmpty()
        database.herdCountDao().setHeadCount(1, com.fildy.data.model.CattleCategory.COW, 999) // user change

        seeder.seedIfEmpty() // e.g. next app launch

        assertEquals(1, database.farmDao().count())
        assertEquals(8, database.farmDao().observeFarmWithPaddocks().first()!!.paddocks.size)
        assertEquals(8, database.rainfallDao().observeSince(LocalDate.of(2026, 1, 1)).first().size)
        assertEquals(999, database.herdCountDao().getHeadCount(1, com.fildy.data.model.CattleCategory.COW))
    }
}
