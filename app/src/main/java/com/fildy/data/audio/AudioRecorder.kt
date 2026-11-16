package com.fildy.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.math.log10

/** Records microphone audio to a file. Requires the RECORD_AUDIO permission. */
interface AudioRecorder {
    /** Starts a new recording, discarding any recording in progress. */
    fun start()

    /** Stops the recording. Returns null if nothing usable was recorded (e.g. stopped instantly). */
    fun stop(): Recording?

    /** Stops and discards the current recording, if any. */
    fun cancel()

    /**
     * Loudness of the microphone since the previous call, from 0 (silence) to 1 (very loud).
     * Returns 0 when not recording. Poll it regularly (e.g. every ~70 ms) to drive a waveform.
     */
    fun level(): Float
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

    override fun level(): Float {
        val amplitude = try {
            recorder?.maxAmplitude ?: return 0f
        } catch (e: IllegalStateException) {
            return 0f
        }
        if (amplitude <= 0) return 0f
        // Map to decibels so quiet speech still moves the bars: -50 dB → 0, 0 dB → 1.
        val decibels = 20 * log10(amplitude / MAX_AMPLITUDE)
        return ((decibels - MIN_DECIBELS) / -MIN_DECIBELS).coerceIn(0f, 1f)
    }

    private companion object {
        const val TAG = "Fildy.AudioRecorder"
        const val MAX_AMPLITUDE = 32_767f
        const val MIN_DECIBELS = -50f
    }
}
