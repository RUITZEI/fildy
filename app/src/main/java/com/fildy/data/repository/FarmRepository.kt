package com.fildy.data.repository

import com.fildy.data.model.CattleCategory
import com.fildy.data.model.Farm
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.model.StockChange
import kotlinx.coroutines.flow.Flow

/** The farm, its paddocks and its herd. */
interface FarmRepository {
    /** The farm, or null before the database is seeded. */
    suspend fun getFarm(): Farm?

    /** The farm with every paddock and its head count per category. Updates on every change. */
    fun observeFarmOverview(): Flow<FarmOverview?>

    /** Latest herd changes, newest first. */
    fun observeStockChanges(limit: Int = 20): Flow<List<StockChange>>

    /**
     * Moves [headCount] animals of [category] between paddocks and records the change.
     * @throws IllegalArgumentException if the source paddock doesn't have enough animals.
     */
    suspend fun moveCattle(
        category: CattleCategory,
        fromPaddockId: Long,
        toPaddockId: Long,
        headCount: Int,
    ): StockChange.Move

    /**
     * Adds, removes or sets the head count of [category] in a paddock. Recorded as a correction
     * from the previous count to the new one, so it can be undone.
     * @throws IllegalArgumentException if the adjustment is invalid: a non-positive amount to add
     * or remove, a negative count to set, or removing more animals than the paddock has.
     */
    suspend fun adjustHeadCount(
        paddockId: Long,
        category: CattleCategory,
        adjustment: HerdAdjustment,
    ): StockChange.Correction

    /**
     * Reverts [change] and removes it from the history.
     * @throws IllegalArgumentException if it can no longer be reverted (e.g. the moved animals
     * were moved again).
     */
    suspend fun undo(change: StockChange)
}
