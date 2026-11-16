package com.fildy.ui.voice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fildy.R
import com.fildy.data.audio.AudioPlayer
import com.fildy.data.audio.AudioRecorder
import com.fildy.data.speech.SpeechToText
import com.fildy.data.speech.TextToSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val recorder: AudioRecorder,
    private val speechToText: SpeechToText,
    private val textToSpeech: TextToSpeech,
    private val player: AudioPlayer,
) : ViewModel() {

    // Switch flag ON/OFF to enable TTS "echoing" to avoid wasting credits.
    private val ECHO_BACK = true

    // TODO: Load the place name from the farm repository.
    private val _uiState = MutableStateFlow(VoiceUiState(placeName = "La Tranquera"))
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    /** Call only once the RECORD_AUDIO permission is granted. */
    fun startRecording() {
        if (_uiState.value.phase != VoicePhase.Idle) {
            Log.w(TAG, "Ignoring start: busy in ${_uiState.value.phase}")
            return
        }
        try {
            recorder.start()
            setPhase(VoicePhase.Recording) { it.copy(transcript = null, errorMessage = null) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            fail(R.string.voice_error_generic)
        }
    }

    fun stopRecording() {
        if (_uiState.value.phase != VoicePhase.Recording) return
        val recording = recorder.stop()
        if (recording == null) {
            fail(R.string.voice_error_too_short)
            return
        }
        setPhase(VoicePhase.Transcribing)
        viewModelScope.launch {
            try {
                val transcription = speechToText.transcribe(recording.file, languageCode = LANGUAGE)
                if (transcription.text.isBlank()) {
                    Log.w(TAG, "Transcription came back empty")
                    fail(R.string.voice_error_nothing_heard)
                    return@launch
                }
                _uiState.update { it.copy(transcript = transcription.text) }

                // For testing: read the transcript back. Replace with the assistant's answer later.
                if (ECHO_BACK) {
                    setPhase(VoicePhase.Speaking)
                    val speech = textToSpeech.synthesize(transcription.text)
                    try {
                        player.play(speech)
                    } finally {
                        speech.delete()
                    }
                }

                setPhase(VoicePhase.Idle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice pipeline failed", e)
                fail(R.string.voice_error_generic)
            } finally {
                recording.file.delete()
            }
        }
    }

    fun onMicrophonePermissionDenied() {
        Log.w(TAG, "Microphone permission denied")
        fail(R.string.voice_error_permission)
    }

    override fun onCleared() {
        recorder.cancel()
        Log.d(TAG, "Cleared")
    }

    private fun setPhase(phase: VoicePhase, extra: (VoiceUiState) -> VoiceUiState = { it }) {
        Log.d(TAG, "Phase ${_uiState.value.phase} → $phase")
        _uiState.update { extra(it).copy(phase = phase) }
    }

    private fun fail(message: Int) = setPhase(VoicePhase.Idle) { it.copy(errorMessage = message) }

    private companion object {
        const val TAG = "Fildy.Voice"
        const val LANGUAGE = "es"
    }
}
