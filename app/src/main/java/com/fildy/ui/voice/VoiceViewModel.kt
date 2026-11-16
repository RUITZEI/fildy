package com.fildy.ui.voice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fildy.R
import com.fildy.data.audio.AudioRecorder
import com.fildy.domain.voice.AnswerVoiceQuestionUseCase
import com.fildy.domain.voice.NothingHeardException
import com.fildy.domain.voice.VoiceQuestionStep
import com.fildy.ui.components.WAVEFORM_BAR_COUNT
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Voice screen: owns the microphone (recording + waveform) and hands the recording to
 * [AnswerVoiceQuestionUseCase], mapping its steps to the screen's phases.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val recorder: AudioRecorder,
    private val answerVoiceQuestion: AnswerVoiceQuestionUseCase,
) : ViewModel() {

    // TODO: Load the place name from the farm repository.
    private val _uiState = MutableStateFlow(VoiceUiState(placeName = "La Tranquera"))
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var levelPolling: Job? = null

    /** Call only once the RECORD_AUDIO permission is granted. */
    fun startRecording() {
        if (_uiState.value.phase != VoicePhase.Idle) {
            Log.w(TAG, "Ignoring start: busy in ${_uiState.value.phase}")
            return
        }
        try {
            recorder.start()
            setPhase(VoicePhase.Recording) {
                it.copy(answer = null, answerCards = emptyList(), errorMessage = null, audioLevels = emptyList())
            }
            startLevelPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            fail(R.string.voice_error_generic)
        }
    }

    fun stopRecording() {
        if (_uiState.value.phase != VoicePhase.Recording) return
        stopLevelPolling()
        val recording = recorder.stop()
        if (recording == null) {
            fail(R.string.voice_error_too_short)
            return
        }
        viewModelScope.launch {
            try {
                answerVoiceQuestion(recording.file).collect { step ->
                    when (step) {
                        VoiceQuestionStep.Transcribing -> setPhase(VoicePhase.Transcribing)
                        is VoiceQuestionStep.Heard -> Log.d(TAG, "Question: \"${step.question}\"")
                        VoiceQuestionStep.Thinking -> setPhase(VoicePhase.Thinking)
                        is VoiceQuestionStep.Answered -> _uiState.update {
                            it.copy(answer = step.text, answerCards = step.data.map { data -> data.toAnswerCard() })
                        }
                        VoiceQuestionStep.Speaking -> setPhase(VoicePhase.Speaking)
                    }
                }
                setPhase(VoicePhase.Idle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: NothingHeardException) {
                Log.w(TAG, e.message.orEmpty())
                fail(R.string.voice_error_nothing_heard)
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
        stopLevelPolling()
        recorder.cancel()
        Log.d(TAG, "Cleared")
    }

    /** Samples the microphone loudness while recording to animate the waveform. */
    private fun startLevelPolling() {
        levelPolling?.cancel()
        levelPolling = viewModelScope.launch {
            while (isActive) {
                delay(LEVEL_POLL_INTERVAL_MS)
                val level = recorder.level()
                _uiState.update { it.copy(audioLevels = (it.audioLevels + level).takeLast(WAVEFORM_BAR_COUNT)) }
            }
        }
    }

    private fun stopLevelPolling() {
        levelPolling?.cancel()
        levelPolling = null
        _uiState.update { it.copy(audioLevels = emptyList()) }
    }

    private fun setPhase(phase: VoicePhase, extra: (VoiceUiState) -> VoiceUiState = { it }) {
        Log.d(TAG, "Phase ${_uiState.value.phase} → $phase")
        _uiState.update { extra(it).copy(phase = phase) }
    }

    private fun fail(message: Int) = setPhase(VoicePhase.Idle) { it.copy(errorMessage = message) }

    private companion object {
        const val TAG = "Fildy.Voice"
        const val LEVEL_POLL_INTERVAL_MS = 70L
    }
}
