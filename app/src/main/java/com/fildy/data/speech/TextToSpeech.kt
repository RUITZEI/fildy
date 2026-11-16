package com.fildy.data.speech

import java.io.File

/** Converts text into spoken audio. Implemented per provider (e.g. ElevenLabs). */
interface TextToSpeech {
    /**
     * Synthesizes [text] into an audio file the app can play.
     *
     * @throws SpeechProviderException if the provider fails.
     */
    suspend fun synthesize(text: String): File
}
