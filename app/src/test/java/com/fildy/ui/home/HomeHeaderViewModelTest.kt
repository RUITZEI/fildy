package com.fildy.ui.home

import com.fildy.data.model.Farm
import com.fildy.data.model.FarmOverview
import com.fildy.ui.home.header.HomeHeaderUiState
import com.fildy.ui.home.header.HomeHeaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeHeaderViewModelTest {

    private val repository = FakeFarmRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun greetsBeforeTheFarmLoads_thenShowsFarmAndLocality() = runTest {
        val vm = HomeHeaderViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(HomeHeaderUiState(userName = "Francisco"), vm.uiState.value)

        repository.overview.value = FarmOverview(Farm(1, "La Tranquera", "Castelli", "Buenos Aires"), emptyList())
        assertEquals(
            HomeHeaderUiState(userName = "Francisco", farmName = "La Tranquera", locality = "Castelli"),
            vm.uiState.value,
        )
    }
}
