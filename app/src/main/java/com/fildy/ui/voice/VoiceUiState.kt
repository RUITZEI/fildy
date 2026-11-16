package com.fildy.ui.voice

import androidx.annotation.StringRes

/** Everything the voice screen needs to render. */
data class VoiceUiState(
    /** Name of the farm shown at the top, e.g. "La Tranquera". */
    val placeName: String,
    val phase: VoicePhase = VoicePhase.Idle,
    /** Text recognized from the last recording, if any. */
    val transcript: String? = null,
    /** Message to show when something went wrong. */
    @StringRes val errorMessage: Int? = null,
    /** Recent microphone loudness (0–1), oldest first, while recording. Drives the waveform. */
    val audioLevels: List<Float> = emptyList(),
) {
    /** True once the user has started talking: the sphere moves to the top. */
    val isCompact: Boolean
        get() = phase != VoicePhase.Idle || transcript != null || errorMessage != null
}

enum class VoicePhase {
    /** Ready: waiting for the user to tap the button. */
    Idle,

    /** The microphone is recording. */
    Recording,

    /** Converting the recording to text. */
    Transcribing,

    /** Reading the transcript back out loud. */
    Speaking,
}
