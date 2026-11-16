package com.fildy.ui.voice

/** Everything the voice screen needs to render. */
data class VoiceUiState(
    val placeName: String,
    val isRecording: Boolean = false,
)
