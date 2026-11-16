package com.fildy.ui.home.rain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fildy.data.model.RainfallRecord
import com.fildy.data.repository.RainfallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RainViewModel @Inject constructor(
    rainfallRepository: RainfallRepository,
    private val clock: Clock,
) : ViewModel() {

    /** Follows the rain records: setting or undoing a day's rain updates the card. */
    val uiState: StateFlow<RainUiState> = rainfallRepository.observeRecent(PERIOD_DAYS.toLong())
        .map { records -> records.toUiState(LocalDate.now(clock)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RainUiState.Loading)

    private fun List<RainfallRecord>.toUiState(today: LocalDate): RainUiState.Success {
        val byDate = associate { it.date to it.millimeters }
        val days = (PERIOD_DAYS - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            DailyRain(date, byDate[date] ?: 0.0)
        }
        val last = filter { it.millimeters > 0 }.maxByOrNull { it.date }
        return RainUiState.Success(
            periodDays = PERIOD_DAYS,
            totalMillimeters = sumOf { it.millimeters },
            lastRain = last?.let { LastRain(it.millimeters, ChronoUnit.DAYS.between(it.date, today)) },
            days = days,
        )
    }

    private companion object {
        const val PERIOD_DAYS = 60
    }
}
