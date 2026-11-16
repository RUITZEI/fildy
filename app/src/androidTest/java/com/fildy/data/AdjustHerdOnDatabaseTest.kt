package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.local.FildyDatabase
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.CattleCategory.BULL
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.CattleCategory.STEER
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.model.StockChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** adjustHeadCount against the real Room database, including undo. */
@RunWith(AndroidJUnit4::class)
class AdjustHerdOnDatabaseTest {

    private lateinit var database: FildyDatabase

    @Before
    fun setUp() = runBlocking {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun count(paddockId: Long, category: CattleCategory) =
        database.herdCountDao().getHeadCount(paddockId, category)

    @Test
    fun add_recordsACorrection_andUndoRestores() = runBlocking {
        val repository = farmRepository(database)
        val change = repository.adjustHeadCount(1, COW, HerdAdjustment.Add(20))

        assertEquals(170, count(1, COW))
        assertEquals(150, change.previousHeadCount)
        assertEquals(listOf<StockChange>(change), repository.observeStockChanges().first())

        repository.undo(change)
        assertEquals(150, count(1, COW))
    }

    @Test
    fun addToACategoryThePaddockDidntHave_createsTheRow() = runBlocking {
        farmRepository(database).adjustHeadCount(1, STEER, HerdAdjustment.Add(5))
        assertEquals(5, count(1, STEER))
    }

    @Test
    fun remove_andUndo() = runBlocking {
        val repository = farmRepository(database)
        val change = repository.adjustHeadCount(8, BULL, HerdAdjustment.Remove(10))
        assertEquals(290, count(8, BULL))

        repository.undo(change)
        assertEquals(300, count(8, BULL))
    }

    @Test
    fun removeMoreThanAvailable_failsWithoutChanges() = runBlocking {
        val repository = farmRepository(database)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.adjustHeadCount(1, COW, HerdAdjustment.Remove(151)) }
        }
        assertEquals(150, count(1, COW))
        assertTrue(repository.observeStockChanges().first().isEmpty())
    }

    @Test
    fun set_andUndo() = runBlocking {
        val repository = farmRepository(database)
        val change = repository.adjustHeadCount(3, COW, HerdAdjustment.Set(155))
        assertEquals(155, count(3, COW))

        repository.undo(change)
        assertEquals(160, count(3, COW))
    }
}
