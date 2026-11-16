package com.fildy.ui.home

import com.fildy.data.model.CattleCategory
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.RainfallRecord
import com.fildy.data.model.RainfallUpdate
import com.fildy.data.model.StockChange
import com.fildy.data.repository.FarmRepository
import com.fildy.data.repository.RainfallRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.time.LocalDate

/** Emits whatever overview the test puts in [overview]. Writes aren't used by the cards. */
class FakeFarmRepository : FarmRepository {
    val overview = MutableStateFlow<FarmOverview?>(null)
    override fun observeFarmOverview(): Flow<FarmOverview?> = overview
    override fun observeStockChanges(limit: Int): Flow<List<StockChange>> = emptyFlow()
    override suspend fun moveCattle(category: CattleCategory, fromPaddockId: Long, toPaddockId: Long, headCount: Int) = TODO()
    override suspend fun correctHeadCount(paddockId: Long, category: CattleCategory, newHeadCount: Int) = TODO()
    override suspend fun undo(change: StockChange) = TODO()
}

/** Emits whatever records the test puts in [records]. */
class FakeRainfallRepository : RainfallRepository {
    val records = MutableStateFlow<List<RainfallRecord>>(emptyList())
    var requestedDays: Long? = null
    override fun observeRecent(days: Long): Flow<List<RainfallRecord>> {
        requestedDays = days
        return records
    }
    override suspend fun setRainfall(date: LocalDate, millimeters: Double): RainfallUpdate = TODO()
    override suspend fun undo(update: RainfallUpdate) = TODO()
    override suspend fun deleteRainfall(id: Long) = TODO()
}
