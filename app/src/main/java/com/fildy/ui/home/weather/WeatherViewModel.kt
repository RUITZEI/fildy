package com.fildy.ui.home.weather

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(clock: Clock) : ViewModel() {

    // TODO: Replace the hardcoded weather with a weather repository. The date is real: today.
    private val _uiState = MutableStateFlow(
        WeatherUiState(
            temperatureCelsius = 24,
            condition = WeatherCondition.CLEAR,
            date = LocalDate.now(clock),
            maxTemperatureCelsius = 27,
            minTemperatureCelsius = 14,
            humidityPercent = 62,
            rainChancePercent = 10,
            windSpeedKmh = 14,
        ),
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
}
