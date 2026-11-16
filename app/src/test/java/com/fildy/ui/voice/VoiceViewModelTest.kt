package com.fildy.ui.voice

import com.fildy.R
import com.fildy.data.audio.AudioPlayer
import com.fildy.data.audio.AudioRecorder
import com.fildy.data.audio.Recording
import com.fildy.data.speech.SpeechProviderException
import com.fildy.data.speech.SpeechToText
import com.fildy.data.speech.TextToSpeech
import com.fildy.data.speech.Transcription
import com.fildy.ui.components.WAVEFORM_BAR_COUNT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val recorder = FakeRecorder()
    private val speechToText = FakeSpeechToText()
    private val textToSpeech = FakeTextToSpeech()
    private val player = FakePlayer()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = VoiceViewModel(recorder, speechToText, textToSpeech, player)

    @Test
    fun recordThenStop_transcribesAndReadsBack() = runTest(dispatcher) {
        speechToText.result = Transcription("Llovieron 18 milímetros", "spa")
        val vm = viewModel()

        vm.startRecording()
        assertEquals(VoicePhase.Recording, vm.uiState.value.phase)

        vm.stopRecording()
        assertEquals(VoicePhase.Transcribing, vm.uiState.value.phase)
        advanceUntilIdle()

        assertEquals("Llovieron 18 milímetros", vm.uiState.value.transcript)
        assertEquals("es", speechToText.lastLanguage)
        assertEquals("Llovieron 18 milímetros", textToSpeech.lastText)
        assertEquals(1, player.plays)
        assertEquals(VoicePhase.Idle, vm.uiState.value.phase)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun whileRecording_audioLevelsFollowTheMicrophone() = runTest(dispatcher) {
        recorder.levels = ArrayDeque(listOf(0.1f, 0.5f, 0.9f))
        val vm = viewModel()
        vm.startRecording()
        advanceTimeBy(250) // 3 samples at 70 ms

        assertEquals(listOf(0.1f, 0.5f, 0.9f), vm.uiState.value.audioLevels)

        vm.stopRecording()
        assertEquals(emptyList<Float>(), vm.uiState.value.audioLevels)
        advanceUntilIdle()
    }

    @Test
    fun audioLevels_keepOnlyTheLatestBars() = runTest(dispatcher) {
        recorder.levels = ArrayDeque(List(30) { it / 30f })
        val vm = viewModel()
        vm.startRecording()
        advanceTimeBy(30 * 70L + 10)

        val levels = vm.uiState.value.audioLevels
        assertEquals(WAVEFORM_BAR_COUNT, levels.size)
        assertEquals(29 / 30f, levels.last())

        vm.stopRecording()
        advanceUntilIdle()
    }

    @Test
    fun tooShortRecording_showsError() {
        recorder.nextRecording = null
        val vm = viewModel()
        vm.startRecording()
        vm.stopRecording()
        assertEquals(VoicePhase.Idle, vm.uiState.value.phase)
        assertEquals(R.string.voice_error_too_short, vm.uiState.value.errorMessage)
    }

    @Test
    fun providerFailure_showsGenericError() = runTest(dispatcher) {
        speechToText.error = SpeechProviderException("HTTP 401")
        val vm = viewModel()
        vm.startRecording()
        vm.stopRecording()
        advanceUntilIdle()
        assertEquals(VoicePhase.Idle, vm.uiState.value.phase)
        assertEquals(R.string.voice_error_generic, vm.uiState.value.errorMessage)
        assertEquals(0, player.plays)
    }

    @Test
    fun emptyTranscript_showsNothingHeard() = runTest(dispatcher) {
        speechToText.result = Transcription("  ")
        val vm = viewModel()
        vm.startRecording()
        vm.stopRecording()
        advanceUntilIdle()
        assertEquals(R.string.voice_error_nothing_heard, vm.uiState.value.errorMessage)
    }

    @Test
    fun permissionDenied_showsPermissionError() {
        val vm = viewModel()
        vm.onMicrophonePermissionDenied()
        assertEquals(R.string.voice_error_permission, vm.uiState.value.errorMessage)
    }

    private class FakeRecorder : AudioRecorder {
        var nextRecording: Recording? = Recording(File.createTempFile("rec", ".m4a"), 1_500)
        override fun start() = Unit
        override fun stop() = nextRecording
        override fun cancel() = Unit
        var levels = ArrayDeque<Float>()
        override fun level() = levels.removeFirstOrNull() ?: 0f
    }

    private class FakeSpeechToText : SpeechToText {
        var result = Transcription("hola")
        var error: Exception? = null
        var lastLanguage: String? = null
        override suspend fun transcribe(audio: File, languageCode: String?): Transcription {
            lastLanguage = languageCode
            error?.let { throw it }
            return result
        }
    }

    private class FakeTextToSpeech : TextToSpeech {
        var lastText: String? = null
        override suspend fun synthesize(text: String): File {
            lastText = text
            return File.createTempFile("tts", ".mp3")
        }
    }

    private class FakePlayer : AudioPlayer {
        var plays = 0
        override suspend fun play(file: File) {
            plays++
        }
    }
}
