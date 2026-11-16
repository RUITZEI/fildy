package com.fildy.data.speech.elevenlabs

import com.fildy.data.speech.SpeechProviderException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val API_KEY_HEADER = "xi-api-key"

internal fun ElevenLabsConfig.requireApiKey(): String {
    if (apiKey.isBlank()) {
        throw SpeechProviderException(
            "ElevenLabs API key is missing. Set ELEVENLABS_API_KEY in local.properties or as an " +
                "environment variable and rebuild.",
        )
    }
    return apiKey
}

/** Runs the request without blocking the caller; cancelling the coroutine cancels the call. */
internal suspend fun OkHttpClient.await(request: Request): Response =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) = continuation.resume(response)

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(SpeechProviderException("Network error: ${e.message}", e))
                }
            },
        )
    }

/** Throws a [SpeechProviderException] with the API's error body when the response failed. */
internal fun Response.throwIfFailed(operation: String) {
    if (isSuccessful) return
    val error = body.string().take(500)
    close()
    throw SpeechProviderException("$operation failed: HTTP $code $error")
}
