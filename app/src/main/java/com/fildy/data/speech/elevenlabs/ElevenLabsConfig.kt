package com.fildy.data.speech.elevenlabs

/** Settings for the ElevenLabs speech APIs. */
data class ElevenLabsConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.elevenlabs.io",
    /** Batch speech-to-text model. */
    val speechToTextModel: String = "scribe_v2",
    /** Text-to-speech model; multilingual so it speaks Spanish naturally. */
    val textToSpeechModel: String = "eleven_multilingual_v2",
    /** Voice used for text-to-speech. Any voice from the ElevenLabs voice library works. */
    val voiceId: String = "QK4xDwo9ESPHA4JNUpX3",
    val outputFormat: String = "mp3_44100_128",
) {
    override fun toString(): String = // Never print the key.
        "ElevenLabsConfig(baseUrl=$baseUrl, stt=$speechToTextModel, tts=$textToSpeechModel, voiceId=$voiceId)"
}
