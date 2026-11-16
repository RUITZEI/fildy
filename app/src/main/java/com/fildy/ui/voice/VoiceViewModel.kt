package com.fildy.ui.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class VoiceViewModel @Inject constructor() : ViewModel() {

    // TODO: Load the place name from the farm repository.
    private val _uiState = MutableStateFlow(VoiceUiState(placeName = "La Tranquera"))
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    // TODO: Start/stop the speech recognizer. For now these only switch the UI state.
    fun startRecording() = _uiState.update { it.copy(isRecording = true) }

    fun stopRecording() = _uiState.update { it.copy(isRecording = false) }
}
