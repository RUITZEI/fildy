package com.fildy.ui.home.header

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fildy.data.repository.FarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeHeaderViewModel @Inject constructor(
    farmRepository: FarmRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeHeaderUiState> = farmRepository.observeFarmOverview()
        .map { overview ->
            HomeHeaderUiState(
                userName = USER_NAME,
                farmName = overview?.farm?.name,
                locality = overview?.farm?.locality,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeHeaderUiState(userName = USER_NAME))

    private companion object {
        // TODO: Take the name from the signed-in user once there are users.
        const val USER_NAME = "Francisco"
    }
}
