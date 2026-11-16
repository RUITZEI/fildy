package com.fildy.data.speech.elevenlabs

import android.util.Log
import com.fildy.data.speech.SpeechProviderException
import com.fildy.data.speech.SpeechToText
import com.fildy.data.speech.Transcription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import kotlin.time.measureTimedValue

/** Batch speech-to-text with ElevenLabs Scribe: `POST /v1/speech-to-text`. */
class ElevenLabsSpeechToText @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val config: ElevenLabsConfig,
) : SpeechToText {

    override suspend fun transcribe(audio: File, languageCode: String?): Transcription {
        val apiKey = config.requireApiKey()
        Log.d(TAG, "Transcribing ${audio.name} (${audio.length()} bytes, language=${languageCode ?: "auto"}, model=${config.speechToTextModel})")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model_id", config.speechToTextModel)
            .apply { languageCode?.let { addFormDataPart("language_code", it) } }
            .addFormDataPart("file", audio.name, audio.asRequestBody(AUDIO_MP4))
            .build()
        val request = Request.Builder()
            .url("${config.baseUrl}/v1/speech-to-text")
            .header(API_KEY_HEADER, apiKey)
            .post(body)
            .build()

        val (transcription, elapsed) = measureTimedValue {
            val response = client.await(request)
            response.throwIfFailed("Speech-to-text")
            val raw = withContext(Dispatchers.IO) { response.use { it.body.string() } }
            val parsed = try {
                json.decodeFromString<SpeechToTextResponse>(raw)
            } catch (e: Exception) {
                throw SpeechProviderException("Unexpected speech-to-text response: ${raw.take(300)}", e)
            }
            Transcription(text = parsed.text.trim(), languageCode = parsed.languageCode)
        }
        Log.i(TAG, "Transcribed in ${elapsed.inWholeMilliseconds} ms (language=${transcription.languageCode}): \"${transcription.text}\"")
        return transcription
    }

    @Serializable
    private data class SpeechToTextResponse(
        val text: String,
        @SerialName("language_code") val languageCode: String? = null,
    )

    private companion object {
        const val TAG = "Fildy.SpeechToText"
        val AUDIO_MP4 = "audio/mp4".toMediaType()
    }
}
