package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.local.FildyDatabase
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.CattleCategory.BULL
import com.fildy.data.model.CattleCategory.CALF
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.CattleCategory.HEIFER
import com.fildy.data.model.CattleCategory.STEER
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.repository.FarmRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmOverviewTest {

    private lateinit var database: FildyDatabase
    private lateinit var repository: FarmRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
        repository = farmRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun overviewContainsEveryPaddockWithAllCategories() = runBlocking {
        val overview = repository.observeFarmOverview().filterNotNull().first()

        assertEquals("La Tranquera", overview.farm.name)
        assertEquals((1L..8L).toList(), overview.paddocks.map { it.paddock.id })
        overview.paddocks.forEach { assertEquals(CattleCategory.entries.toSet(), it.headCounts.keys) }

        val lote1 = overview.paddocks.first { it.paddock.name == "Lote 1" }
        assertEquals(mapOf(COW to 150, HEIFER to 0, CALF to 148, STEER to 0, BULL to 50), lote1.headCounts)
        assertEquals(348, lote1.totalHeadCount)

        assertEquals(2247, overview.totalHeadCount)
        assertEquals(
            mapOf(COW to 450, HEIFER to 448, CALF to 452, STEER to 447, BULL to 450),
            overview.headCountsByCategory,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun overviewUpdatesAfterChanges() = runBlocking {
        val updates = repository.observeFarmOverview().filterNotNull().produceIn(this)
        updates.awaitMatching { it.cows(paddockId = 1) == 150 }

        repository.moveCattle(COW, fromPaddockId = 1, toPaddockId = 4, headCount = 30)
        updates.awaitMatching { it.cows(1) == 120 && it.cows(4) == 30 }

        repository.adjustHeadCount(paddockId = 2, category = COW, adjustment = HerdAdjustment.Set(135))
        val latest = updates.awaitMatching { it.cows(2) == 135 }
        assertEquals(2242, latest.totalHeadCount) // 2247 - 5 from the correction; the move kept the total

        updates.cancel()
    }

    private fun FarmOverview.cows(paddockId: Long) = paddocks.first { it.paddock.id == paddockId }.headCount(COW)

    /** Waits (up to 5 s) for an emission matching [predicate]; fails the test on timeout. */
    private suspend fun ReceiveChannel<FarmOverview>.awaitMatching(predicate: (FarmOverview) -> Boolean): FarmOverview =
        withTimeout(5_000) {
            var item = receive()
            while (!predicate(item)) item = receive()
            item
        }
}
