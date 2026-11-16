package com.fildy.ui.home

import com.fildy.data.model.RainfallRecord
import com.fildy.ui.home.rain.LastRain
import com.fildy.ui.home.rain.RainUiState
import com.fildy.ui.home.rain.RainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RainViewModelTest {

    private val repository = FakeRainfallRepository()
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun record(daysAgo: Long, mm: Double) =
        RainfallRecord(id = daysAgo, farmId = 1, date = today.minusDays(daysAgo), millimeters = mm, recordedAt = Instant.EPOCH)

    private fun kotlinx.coroutines.test.TestScope.viewModel() = RainViewModel(repository, clock).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun asksForTheLast60Days() = runTest {
        viewModel()
        assertEquals(60L, repository.requestedDays)
    }

    @Test
    fun totalsLastRainAndOneBarPerDay() = runTest {
        val vm = viewModel()
        repository.records.value = listOf(record(1, 18.0), record(9, 19.0), record(56, 24.0))

        val state = vm.uiState.value as RainUiState.Success
        assertEquals(61.0, state.totalMillimeters, 0.0)
        assertEquals(LastRain(18.0, daysAgo = 1), state.lastRain)
        assertEquals(60, state.days.size)
        assertEquals(today.minusDays(59), state.days.first().date) // oldest first
        assertEquals(today, state.days.last().date)
        assertEquals(18.0, state.days[58].millimeters, 0.0) // yesterday
        assertEquals(0.0, state.days.last().millimeters, 0.0) // dry today
        assertEquals(3, state.days.count { it.millimeters > 0 })
    }

    @Test
    fun noRain_hasNoLastRain() = runTest {
        val vm = viewModel()
        repository.records.value = emptyList()

        val state = vm.uiState.value as RainUiState.Success
        assertNull(state.lastRain)
        assertEquals(0.0, state.totalMillimeters, 0.0)
    }

    @Test
    fun rainToday_isZeroDaysAgo() = runTest {
        val vm = viewModel()
        repository.records.value = listOf(record(0, 5.5), record(3, 10.0))

        assertEquals(LastRain(5.5, daysAgo = 0), (vm.uiState.value as RainUiState.Success).lastRain)
    }
}
