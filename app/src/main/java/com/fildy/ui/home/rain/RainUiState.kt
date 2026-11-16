package com.fildy.ui.home.rain

import java.time.LocalDate

/** What the "Lluvia · 60 días" card shows. */
sealed interface RainUiState {
    data object Loading : RainUiState

    data class Success(
        /** Length of the window, e.g. 60. */
        val periodDays: Int,
        val totalMillimeters: Double,
        /** Most recent day with rain, or null if it didn't rain in the period. */
        val lastRain: LastRain?,
        /** One entry per day, oldest first, [periodDays] long; 0 mm on dry days. */
        val days: List<DailyRain>,
    ) : RainUiState
}

data class LastRain(val millimeters: Double, val daysAgo: Long)

data class DailyRain(val date: LocalDate, val millimeters: Double)
