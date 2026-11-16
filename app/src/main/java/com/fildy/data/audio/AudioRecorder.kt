package com.fildy.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Records microphone audio to a file. Requires the RECORD_AUDIO permission. */
interface AudioRecorder {
    /** Starts a new recording, discarding any recording in progress. */
    fun start()

    /** Stops the recording. Returns null if nothing usable was recorded (e.g. stopped instantly). */
    fun stop(): Recording?

    /** Stops and discards the current recording, if any. */
    fun cancel()
}

data class Recording(val file: File, val durationMs: Long)

/** [AudioRecorder] backed by [MediaRecorder]: mono AAC in an MPEG-4 (.m4a) file. */
class MediaAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAt = 0L

    override fun start() {
        cancel()
        val file = File(context.cacheDir, "recording-${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(96_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recording", e)
            mediaRecorder.release()
            file.delete()
            throw e
        }
        recorder = mediaRecorder
        output = file
        startedAt = SystemClock.elapsedRealtime()
        Log.i(TAG, "Recording started → ${file.name}")
    }

    override fun stop(): Recording? {
        val mediaRecorder = recorder ?: return null.also { Log.w(TAG, "stop() called while not recording") }
        val file = output
        val duration = SystemClock.elapsedRealtime() - startedAt
        recorder = null
        output = null
        return try {
            mediaRecorder.stop()
            Log.i(TAG, "Recording stopped: ${duration} ms, ${file?.length()} bytes")
            file?.let { Recording(it, duration) }
        } catch (e: RuntimeException) {
            // MediaRecorder throws when stopped before any audio was captured.
            Log.w(TAG, "Recording too short to keep (${duration} ms)", e)
            file?.delete()
            null
        } finally {
            mediaRecorder.release()
        }
    }

    override fun cancel() {
        val mediaRecorder = recorder ?: return
        Log.i(TAG, "Recording cancelled")
        runCatching { mediaRecorder.stop() }
        mediaRecorder.release()
        output?.delete()
        recorder = null
        output = null
    }

    private companion object {
        const val TAG = "Fildy.AudioRecorder"
    }
}
