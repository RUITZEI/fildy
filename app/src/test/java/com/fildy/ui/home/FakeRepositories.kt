package com.fildy.ui.home

import com.fildy.data.model.CattleCategory
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.model.RainfallRecord
import com.fildy.data.model.RainfallUpdate
import com.fildy.data.model.StockChange
import com.fildy.data.repository.FarmRepository
import com.fildy.data.repository.RainfallRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.time.Instant
import java.time.LocalDate

/** Emits whatever overview the test puts in [overview]. Writes aren't used by the cards. */
class FakeFarmRepository : FarmRepository {
    val overview = MutableStateFlow<FarmOverview?>(null)
    override suspend fun getFarm() = overview.value?.farm
    override fun observeFarmOverview(): Flow<FarmOverview?> = overview
    override fun observeStockChanges(limit: Int): Flow<List<StockChange>> = emptyFlow()
    val moves = mutableListOf<StockChange.Move>()

    /** Moves animals inside [overview], like the real repository (which also validates stock). */
    override suspend fun moveCattle(
        category: CattleCategory,
        fromPaddockId: Long,
        toPaddockId: Long,
        headCount: Int,
    ): StockChange.Move {
        val current = checkNotNull(overview.value)
        require(current.paddocks.first { it.paddock.id == fromPaddockId }.headCount(category) >= headCount)
        overview.value = current.copy(
            paddocks = current.paddocks.map { stock ->
                val delta = when (stock.paddock.id) {
                    fromPaddockId -> -headCount
                    toPaddockId -> headCount
                    else -> 0
                }
                stock.copy(headCounts = stock.headCounts + (category to stock.headCount(category) + delta))
            },
        )
        return StockChange.Move(
            id = moves.size + 1L,
            category = category,
            recordedAt = Instant.parse("2026-11-16T22:48:00Z"),
            fromPaddockId = fromPaddockId,
            toPaddockId = toPaddockId,
            headCount = headCount,
        ).also { moves += it }
    }
    val adjustments = mutableListOf<StockChange.Correction>()

    /** Adjusts a count inside [overview], validating like the real repository. */
    override suspend fun adjustHeadCount(
        paddockId: Long,
        category: CattleCategory,
        adjustment: HerdAdjustment,
    ): StockChange.Correction {
        val current = checkNotNull(overview.value)
        val previous = current.paddocks.first { it.paddock.id == paddockId }.headCount(category)
        val new = when (adjustment) {
            is HerdAdjustment.Add -> previous + adjustment.headCount
            is HerdAdjustment.Remove -> previous - adjustment.headCount.also { require(it <= previous) }
            is HerdAdjustment.Set -> adjustment.headCount
        }
        overview.value = current.copy(
            paddocks = current.paddocks.map { stock ->
                if (stock.paddock.id == paddockId) stock.copy(headCounts = stock.headCounts + (category to new)) else stock
            },
        )
        return StockChange.Correction(
            id = adjustments.size + 1L,
            category = category,
            recordedAt = Instant.parse("2026-11-16T22:48:00Z"),
            paddockId = paddockId,
            previousHeadCount = previous,
            newHeadCount = new,
        ).also { adjustments += it }
    }
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
    override suspend fun getRainfall(date: LocalDate) = records.value.firstOrNull { it.date == date }
    override suspend fun getRainfallBetween(from: LocalDate, to: LocalDate) =
        records.value.filter { it.date in from..to }.sortedBy { it.date }
    /** One record per day, like the real repository: replaces the day's record if it has one. */
    override suspend fun setRainfall(date: LocalDate, millimeters: Double): RainfallUpdate {
        val previous = records.value.firstOrNull { it.date == date }
        val record = RainfallRecord(
            id = previous?.id ?: (records.value.maxOfOrNull { it.id } ?: 0) + 1,
            farmId = 1,
            date = date,
            millimeters = millimeters,
            recordedAt = Instant.parse("2026-11-16T22:48:00Z"),
        )
        records.value = records.value.filterNot { it.date == date } + record
        return RainfallUpdate(record, previous)
    }
    override suspend fun undo(update: RainfallUpdate) = TODO()
    override suspend fun deleteRainfall(id: Long) = TODO()
}
