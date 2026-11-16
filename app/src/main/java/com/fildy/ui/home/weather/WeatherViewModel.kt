package com.fildy.ui.home.weather

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class WeatherViewModel @Inject constructor() : ViewModel() {

    // TODO: Replace the hardcoded data with a weather repository.
    private val _uiState = MutableStateFlow(
        WeatherUiState(
            temperatureCelsius = 24,
            condition = WeatherCondition.CLEAR,
            date = LocalDate.of(2026, 11, 15),
            maxTemperatureCelsius = 27,
            minTemperatureCelsius = 14,
            humidityPercent = 62,
            rainChancePercent = 10,
            windSpeedKmh = 14,
        ),
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
}
