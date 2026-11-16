package com.fildy.data.speech

import java.io.File

/** Converts recorded speech into text. Implemented per provider (e.g. ElevenLabs). */
interface SpeechToText {
    /**
     * Transcribes the audio in [audio].
     *
     * @param languageCode ISO-639 code of the spoken language (e.g. "es"), or null to auto-detect.
     * @throws SpeechProviderException if the provider fails.
     */
    suspend fun transcribe(audio: File, languageCode: String? = null): Transcription
}

data class Transcription(
    val text: String,
    /** Language the provider detected, if it reports one. */
    val languageCode: String? = null,
)
