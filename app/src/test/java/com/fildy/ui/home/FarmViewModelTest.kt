package com.fildy.ui.home

import com.fildy.data.model.CattleCategory
import com.fildy.data.model.CattleCategory.BULL
import com.fildy.data.model.CattleCategory.CALF
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.CattleCategory.HEIFER
import com.fildy.data.model.CattleCategory.STEER
import com.fildy.data.model.Farm
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.Paddock
import com.fildy.data.model.PaddockStock
import com.fildy.ui.home.farm.CategoryCount
import com.fildy.ui.home.farm.FarmUiState
import com.fildy.ui.home.farm.FarmViewModel
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
class FarmViewModelTest {

    private val repository = FakeFarmRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadingUntilTheFarmExists() = runTest {
        val vm = FarmViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(FarmUiState.Loading, vm.uiState.value)
    }

    @Test
    fun showsPaddocksTotalsAndEveryCategoryInOrder() = runTest {
        val vm = FarmViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        repository.overview.value = overview(
            mapOf(COW to 150, CALF to 148, BULL to 50),
            mapOf(HEIFER to 230),
            mapOf(STEER to 240, BULL to 300),
        )

        assertEquals(
            FarmUiState.Success(
                paddockCount = 3,
                totalHeadCount = 1118,
                headCounts = listOf(
                    CategoryCount(COW, 150),
                    CategoryCount(HEIFER, 230),
                    CategoryCount(CALF, 148),
                    CategoryCount(STEER, 240),
                    CategoryCount(BULL, 350),
                ),
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun updatesWhenTheHerdChanges() = runTest {
        val vm = FarmViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        repository.overview.value = overview(mapOf(COW to 150))
        repository.overview.value = overview(mapOf(COW to 120), mapOf(COW to 30)) // a move

        val state = vm.uiState.value as FarmUiState.Success
        assertEquals(2, state.paddockCount)
        assertEquals(150, state.totalHeadCount)
    }

    /** A farm with one paddock per map of counts; missing categories are 0. */
    private fun overview(vararg paddocks: Map<CattleCategory, Int>) = FarmOverview(
        farm = Farm(1, "La Tranquera", "Castelli", "Buenos Aires"),
        paddocks = paddocks.mapIndexed { i, counts ->
            PaddockStock(
                paddock = Paddock(i + 1L, 1, "Lote ${i + 1}"),
                headCounts = CattleCategory.entries.associateWith { counts[it] ?: 0 },
            )
        },
    )
}
