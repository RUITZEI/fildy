package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.local.FildyDatabase
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.CattleCategory.HEIFER
import com.fildy.data.model.StockChange
import com.fildy.data.repository.FarmRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmRepositoryImplTest {

    private lateinit var database: FildyDatabase
    private lateinit var repository: FarmRepositoryImpl

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
        repository = farmRepository(database)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun count(paddockId: Long, category: com.fildy.data.model.CattleCategory) =
        database.herdCountDao().getHeadCount(paddockId, category)

    @Test
    fun moveCattle_updatesBothPaddocksAndRecordsTheChange() = runTest {
        val move = repository.moveCattle(COW, fromPaddockId = 1, toPaddockId = 4, headCount = 30)

        assertEquals(120, count(1, COW))
        assertEquals(30, count(4, COW)) // Lote 4 had no cows: row created
        assertEquals(listOf<StockChange>(move), repository.observeStockChanges().first())
        assertEquals(2247, repository.observeFarmOverview().first()?.totalHeadCount) // nothing lost
    }

    @Test
    fun moveCattle_movingEverythingRemovesTheRow() = runTest {
        repository.moveCattle(HEIFER, fromPaddockId = 5, toPaddockId = 4, headCount = 218)

        val lote5 = repository.observeFarmOverview().first()!!.paddocks.first { it.paddock.id == 5L }
        assertEquals(0, lote5.headCount(HEIFER))
        // The row is removed, not kept at 0.
        val rawLote5 = database.farmDao().observeFarmWithPaddocks().first()!!.paddocks.first { it.paddock.id == 5L }
        assertTrue(rawLote5.herd.none { it.category == HEIFER })
        assertEquals(448, count(4, HEIFER))
    }

    @Test
    fun moveCattle_notEnoughAnimals_failsWithoutChanges() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.moveCattle(COW, 1, 2, 151) }
        }
        assertEquals(150, count(1, COW))
        assertEquals(140, count(2, COW))
        assertTrue(repository.observeStockChanges().first().isEmpty())
    }

    @Test
    fun undoMove_restoresCounts() = runTest {
        val move = repository.moveCattle(COW, 1, 2, 50)
        repository.undo(move)

        assertEquals(150, count(1, COW))
        assertEquals(140, count(2, COW))
        assertTrue(repository.observeStockChanges().first().isEmpty())
    }

    @Test
    fun correction_andUndo() = runTest {
        val correction = repository.correctHeadCount(paddockId = 3, category = COW, newHeadCount = 155)
        assertEquals(160, correction.previousHeadCount)
        assertEquals(155, count(3, COW))

        repository.undo(correction)
        assertEquals(160, count(3, COW))
    }

    @Test
    fun undoTwice_fails() = runTest {
        val move = repository.moveCattle(COW, 1, 2, 10)
        repository.undo(move)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.undo(move) }
        }
    }
}
