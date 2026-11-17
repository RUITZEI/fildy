package com.fildy.ui.voice

import androidx.annotation.StringRes

/** Everything the voice screen needs to render. */
data class VoiceUiState(
    /** Name of the farm shown at the top, e.g. "La Tranquera". */
    val placeName: String,
    val phase: VoicePhase = VoicePhase.Idle,
    /** The assistant's answer to the last question, if any. */
    val answer: String? = null,
    /** Cards with the data behind [answer] (e.g. rain for the asked day), in order. */
    val answerCards: List<AnswerCard> = emptyList(),
    /** Message to show when something went wrong. */
    @StringRes val errorMessage: Int? = null,
    /** Recent microphone loudness (0–1), oldest first, while recording. Drives the waveform. */
    val audioLevels: List<Float> = emptyList(),
    /**
     * Show the "Te escucho…" prompt while recording. Only for the first recording since the screen
     * opened; follow-ups don't need the explanation again.
     */
    val showRecordingPrompt: Boolean = true,
) {
    /** True once the user has started talking: the sphere moves to the top. */
    val isCompact: Boolean
        get() = phase != VoicePhase.Idle || answer != null || errorMessage != null
}

enum class VoicePhase {
    /** Ready: waiting for the user to tap the button. */
    Idle,

    /** The microphone is recording. */
    Recording,

    /** Converting the recording to text. */
    Transcribing,

    /** The assistant is working out the answer (may call tools). */
    Thinking,

    /** Reading the answer out loud. */
    Speaking,
}
