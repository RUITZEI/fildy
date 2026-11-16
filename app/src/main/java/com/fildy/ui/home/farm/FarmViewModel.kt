package com.fildy.ui.home.farm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.FarmOverview
import com.fildy.data.repository.FarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FarmViewModel @Inject constructor(
    farmRepository: FarmRepository,
) : ViewModel() {

    val uiState: StateFlow<FarmUiState> = farmRepository.observeFarmOverview()
        .map { overview -> overview?.toUiState() ?: FarmUiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmUiState.Loading)
}

private fun FarmOverview.toUiState(): FarmUiState.Success {
    val byCategory = headCountsByCategory
    return FarmUiState.Success(
        paddockCount = paddocks.size,
        totalHeadCount = totalHeadCount,
        headCounts = CattleCategory.entries.map { CategoryCount(it, byCategory[it] ?: 0) },
    )
}
