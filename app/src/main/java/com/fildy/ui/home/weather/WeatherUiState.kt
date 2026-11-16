package com.fildy.ui.home.weather

import java.time.LocalDate

/** Everything the weather section on the home screen needs to render. */
data class WeatherUiState(
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val date: LocalDate,
    val maxTemperatureCelsius: Int,
    val minTemperatureCelsius: Int,
    val humidityPercent: Int,
    val rainChancePercent: Int,
    val windSpeedKmh: Int,
)

enum class WeatherCondition {
    CLEAR,
}
