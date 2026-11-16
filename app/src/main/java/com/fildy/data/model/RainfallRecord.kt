package com.fildy.data.model

import java.time.Instant
import java.time.LocalDate

/** Rain measured for the whole farm on [date]. There's at most one record per day. */
data class RainfallRecord(
    val id: Long,
    val farmId: Long,
    val date: LocalDate,
    val millimeters: Double,
    val recordedAt: Instant,
)

/** Result of setting a day's rain: the saved [record] and what it replaced, for undo. */
data class RainfallUpdate(
    val record: RainfallRecord,
    /** The day's previous record, or null if the day had no rain recorded. */
    val previous: RainfallRecord?,
)
