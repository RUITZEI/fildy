package com.fildy.data.speech.elevenlabs

import android.content.Context
import android.util.Log
import com.fildy.data.speech.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import kotlin.time.measureTimedValue

/** Text-to-speech with ElevenLabs: `POST /v1/text-to-speech/{voice_id}`, saved as an MP3 file. */
class ElevenLabsTextToSpeech @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
    private val config: ElevenLabsConfig,
) : TextToSpeech {

    override suspend fun synthesize(text: String): File {
        val apiKey = config.requireApiKey()
        Log.d(TAG, "Synthesizing ${text.length} chars (voice=${config.voiceId}, model=${config.textToSpeechModel})")

        val payload = buildJsonObject {
            put("text", text)
            put("model_id", config.textToSpeechModel)
        }
        val request = Request.Builder()
            .url("${config.baseUrl}/v1/text-to-speech/${config.voiceId}?output_format=${config.outputFormat}")
            .header(API_KEY_HEADER, apiKey)
            .header("Accept", "audio/mpeg")
            .post(json.encodeToString(payload).toRequestBody(JSON))
            .build()

        val (file, elapsed) = measureTimedValue {
            val response = client.await(request)
            response.throwIfFailed("Text-to-speech")
            withContext(Dispatchers.IO) {
                val output = File(context.cacheDir, "tts-${System.currentTimeMillis()}.mp3")
                response.use { r -> output.outputStream().use { r.body.byteStream().copyTo(it) } }
                output
            }
        }
        Log.i(TAG, "Synthesized ${file.length()} bytes in ${elapsed.inWholeMilliseconds} ms → ${file.name}")
        return file
    }

    private companion object {
        const val TAG = "Fildy.TextToSpeech"
        val JSON = "application/json".toMediaType()
    }
}
