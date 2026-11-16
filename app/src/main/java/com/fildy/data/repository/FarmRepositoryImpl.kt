package com.fildy.data.repository

import android.util.Log
import com.fildy.data.local.TransactionRunner
import com.fildy.data.local.dao.FarmDao
import com.fildy.data.local.dao.HerdCountDao
import com.fildy.data.local.dao.StockChangeDao
import com.fildy.data.local.entity.toDomain
import com.fildy.data.local.entity.toEntity
import com.fildy.data.local.relation.toDomain
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.StockChange
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [FarmRepository] backed by the local Room database. */
class FarmRepositoryImpl @Inject constructor(
    private val farmDao: FarmDao,
    private val herdDao: HerdCountDao,
    private val changeDao: StockChangeDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : FarmRepository {

    override fun observeFarmOverview(): Flow<FarmOverview?> =
        farmDao.observeFarmWithPaddocks().map { it?.toDomain() }

    override fun observeStockChanges(limit: Int): Flow<List<StockChange>> =
        changeDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun moveCattle(
        category: CattleCategory,
        fromPaddockId: Long,
        toPaddockId: Long,
        headCount: Int,
    ): StockChange.Move = transaction {
        transfer(category, fromPaddockId, toPaddockId, headCount)
        val change = StockChange.Move(
            id = 0,
            category = category,
            recordedAt = clock.instant(),
            fromPaddockId = fromPaddockId,
            toPaddockId = toPaddockId,
            headCount = headCount,
        )
        change.copy(id = changeDao.insert(change.toEntity()))
    }.also { Log.i(TAG, "Moved ${it.headCount} $category: paddock ${it.fromPaddockId} → ${it.toPaddockId}") }

    override suspend fun correctHeadCount(
        paddockId: Long,
        category: CattleCategory,
        newHeadCount: Int,
    ): StockChange.Correction = transaction {
        require(newHeadCount >= 0) { "Head count can't be negative: $newHeadCount" }
        val previous = herdDao.getHeadCount(paddockId, category)
        herdDao.setHeadCount(paddockId, category, newHeadCount)
        val change = StockChange.Correction(
            id = 0,
            category = category,
            recordedAt = clock.instant(),
            paddockId = paddockId,
            previousHeadCount = previous,
            newHeadCount = newHeadCount,
        )
        change.copy(id = changeDao.insert(change.toEntity()))
    }.also { Log.i(TAG, "Corrected $category in paddock $paddockId: ${it.previousHeadCount} → ${it.newHeadCount}") }

    override suspend fun undo(change: StockChange) {
        transaction {
            requireNotNull(changeDao.getById(change.id)) { "Change ${change.id} doesn't exist (already undone?)" }
            when (change) {
                is StockChange.Move ->
                    transfer(change.category, change.toPaddockId, change.fromPaddockId, change.headCount)
                is StockChange.Correction ->
                    herdDao.setHeadCount(change.paddockId, change.category, change.previousHeadCount)
            }
            changeDao.delete(change.id)
        }
        Log.i(TAG, "Undid change ${change.id}: $change")
    }

    /** Moves animals between paddocks. Must run inside a transaction. */
    private suspend fun transfer(category: CattleCategory, from: Long, to: Long, headCount: Int) {
        require(headCount > 0) { "Head count to move must be positive: $headCount" }
        require(from != to) { "Source and destination paddock are the same: $from" }
        val available = herdDao.getHeadCount(from, category)
        require(available >= headCount) {
            "Paddock $from has only $available $category, can't move $headCount"
        }
        herdDao.setHeadCount(from, category, available - headCount)
        herdDao.setHeadCount(to, category, herdDao.getHeadCount(to, category) + headCount)
    }

    private companion object {
        const val TAG = "Fildy.FarmRepository"
    }
}
