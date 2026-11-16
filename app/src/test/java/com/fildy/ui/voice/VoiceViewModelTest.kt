package com.fildy.ui.voice

import com.fildy.R
import com.fildy.data.assistant.AssistantData
import com.fildy.data.audio.AudioRecorder
import com.fildy.data.audio.Recording
import com.fildy.data.speech.SpeechProviderException
import com.fildy.domain.voice.AnswerVoiceQuestionUseCase
import com.fildy.domain.voice.FakeAssistant
import com.fildy.domain.voice.FakePlayer
import com.fildy.domain.voice.FakeSpeechToText
import com.fildy.domain.voice.FakeTextToSpeech
import com.fildy.domain.voice.VoiceAssistantSettings
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val recorder = FakeRecorder()
    private val speechToText = FakeSpeechToText()
    private val assistant = FakeAssistant()
    private val textToSpeech = FakeTextToSpeech()
    private val player = FakePlayer()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(speakAnswers: Boolean = true) = VoiceViewModel(
        recorder,
        AnswerVoiceQuestionUseCase(speechToText, assistant, textToSpeech, player, VoiceAssistantSettings(speakAnswers)),
    )

    @Test
    fun recordThenStop_showsTheAssistantsAnswerAndSpeaksIt() = runTest(dispatcher) {
        speechToText.text = "¿Cuánto llovió ayer?"
        assistant.reply = "Ayer llovieron 18 milímetros."
        assistant.data = listOf(AssistantData.RainOnDate(LocalDate.of(2026, 11, 15), 18.0))
        val vm = viewModel()

        vm.startRecording()
        assertEquals(VoicePhase.Recording, vm.uiState.value.phase)

        vm.stopRecording()
        advanceUntilIdle()

        assertEquals("¿Cuánto llovió ayer?", assistant.lastQuestion)
        assertEquals("Ayer llovieron 18 milímetros.", vm.uiState.value.answer)
        assertEquals(listOf(AnswerCard.RainDay(LocalDate.of(2026, 11, 15), 18.0)), vm.uiState.value.answerCards)
        assertEquals("Ayer llovieron 18 milímetros.", textToSpeech.lastText)
        assertEquals(1, player.plays)
        assertEquals(VoicePhase.Idle, vm.uiState.value.phase)
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(recorder.lastFile.exists()) // recording deleted
    }

    @Test
    fun speakAnswersOff_showsTheAnswerWithoutSpeaking() = runTest(dispatcher) {
        val vm = viewModel(speakAnswers = false)
        vm.startRecording()
        vm.stopRecording()
        advanceUntilIdle()

        assertEquals(assistant.reply, vm.uiState.value.answer)
        assertEquals(0, player.plays)
        assertEquals(VoicePhase.Idle, vm.uiState.value.phase)
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
    fun assistantFailure_showsGenericError() = runTest(dispatcher) {
        assistant.error = SpeechProviderException("HTTP 401")
        val vm = viewModel()
        vm.startRecording()
        vm.stopRecording()
        advanceUntilIdle()

        assertEquals(VoicePhase.Idle, vm.uiState.value.phase)
        assertEquals(R.string.voice_error_generic, vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.answer)
        assertEquals(0, player.plays)
    }

    @Test
    fun emptyTranscript_showsNothingHeard() = runTest(dispatcher) {
        speechToText.text = "  "
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
        val lastFile: File = File.createTempFile("rec", ".m4a")
        var nextRecording: Recording? = Recording(lastFile, 1_500)
        var levels = ArrayDeque<Float>()
        override fun start() = Unit
        override fun stop() = nextRecording
        override fun cancel() = Unit
        override fun level() = levels.removeFirstOrNull() ?: 0f
    }
}
