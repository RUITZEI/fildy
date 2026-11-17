package com.fildy.ui.home

import com.fildy.ui.home.weather.WeatherViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeatherViewModelTest {

    @Test
    fun showsToday_inTheDevicesTimeZone() {
        // 01:30 UTC on Nov 17 is still Nov 16 in Buenos Aires (UTC-3).
        val clock = Clock.fixed(Instant.parse("2026-11-17T01:30:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

        assertEquals(LocalDate.of(2026, 11, 16), WeatherViewModel(clock).uiState.value.date)
    }
}
