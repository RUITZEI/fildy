package com.fildy.data.repository

import com.fildy.data.model.RainfallRecord
import com.fildy.data.model.RainfallUpdate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Rain measured for the whole farm, at most one record per day. */
interface RainfallRepository {
    /** Rain in the last [days] days (including today), newest first. */
    fun observeRecent(days: Long = 60): Flow<List<RainfallRecord>>

    /**
     * Sets the rain for [date]. If the day already has a record, it's replaced (e.g. a
     * correction); the returned [RainfallUpdate] keeps the previous one so it can be undone.
     */
    /** The rain recorded on [date], or null if none was recorded. */
    suspend fun getRainfall(date: LocalDate): RainfallRecord?

    /** Rain recorded from [from] to [to] (both included), oldest first. Dry days aren't included. */
    suspend fun getRainfallBetween(from: LocalDate, to: LocalDate): List<RainfallRecord>

    suspend fun setRainfall(date: LocalDate, millimeters: Double): RainfallUpdate

    /**
     * Reverts [update]: restores the previous record, or removes the day's record if there was
     * none before.
     * @throws IllegalArgumentException if the day changed again after [update].
     */
    suspend fun undo(update: RainfallUpdate)

    /** Removes a record. */
    suspend fun deleteRainfall(id: Long)
}
