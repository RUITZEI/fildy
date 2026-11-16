package com.fildy.data.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Plays an audio file. */
interface AudioPlayer {
    /** Plays [file] and suspends until playback finishes. Cancelling stops playback. */
    suspend fun play(file: File)
}

/** [AudioPlayer] backed by [MediaPlayer]. */
class MediaAudioPlayer @Inject constructor() : AudioPlayer {

    override suspend fun play(file: File) = suspendCancellableCoroutine { continuation ->
        val player = MediaPlayer()
        continuation.invokeOnCancellation {
            Log.i(TAG, "Playback cancelled")
            player.release()
        }
        player.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setOnPreparedListener {
                Log.i(TAG, "Playback started: ${file.name} (${it.duration} ms)")
                it.start()
            }
            setOnCompletionListener {
                Log.i(TAG, "Playback finished")
                it.release()
                if (continuation.isActive) continuation.resume(Unit)
            }
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "Playback error what=$what extra=$extra")
                mp.release()
                if (continuation.isActive) continuation.resumeWithException(IOException("MediaPlayer error $what/$extra"))
                true
            }
            setDataSource(file.absolutePath)
            prepareAsync()
        }
    }

    private companion object {
        const val TAG = "Fildy.AudioPlayer"
    }
}
