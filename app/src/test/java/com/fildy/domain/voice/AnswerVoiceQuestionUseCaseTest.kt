package com.fildy.domain.voice

import com.fildy.data.assistant.Assistant
import com.fildy.data.assistant.AssistantAnswer
import com.fildy.data.assistant.AssistantData
import com.fildy.data.audio.AudioPlayer
import com.fildy.data.speech.SpeechToText
import com.fildy.data.speech.TextToSpeech
import com.fildy.data.speech.Transcription
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class AnswerVoiceQuestionUseCaseTest {

    private val speechToText = FakeSpeechToText()
    private val assistant = FakeAssistant()
    private val textToSpeech = FakeTextToSpeech()
    private val player = FakePlayer()

    private fun useCase(speakAnswers: Boolean = true) =
        AnswerVoiceQuestionUseCase(speechToText, assistant, textToSpeech, player, VoiceAssistantSettings(speakAnswers))

    private val recording = File.createTempFile("rec", ".m4a")

    @Test
    fun transcribesAsksAndSpeaksTheAnswer() = runTest {
        speechToText.text = "¿Cuánto llovió ayer?"
        assistant.reply = "Ayer llovieron 18 milímetros."
        assistant.data = listOf(AssistantData.RainOnDate(java.time.LocalDate.of(2026, 11, 15), 18.0))

        val steps = useCase().invoke(recording).toList()

        assertEquals(
            listOf(
                VoiceQuestionStep.Transcribing,
                VoiceQuestionStep.Heard("¿Cuánto llovió ayer?"),
                VoiceQuestionStep.Thinking,
                VoiceQuestionStep.Answered(
                    "Ayer llovieron 18 milímetros.",
                    listOf(AssistantData.RainOnDate(java.time.LocalDate.of(2026, 11, 15), 18.0)),
                ),
                VoiceQuestionStep.Speaking,
            ),
            steps,
        )
        assertEquals("es", speechToText.lastLanguage)
        assertEquals("¿Cuánto llovió ayer?", assistant.lastQuestion)
        assertEquals("Ayer llovieron 18 milímetros.", textToSpeech.lastText) // the answer, not the question
        assertEquals(1, player.plays)
        assertFalse(textToSpeech.lastFile!!.exists()) // synthesized audio is cleaned up
    }

    @Test
    fun speakAnswersOff_skipsTextToSpeech() = runTest {
        val steps = useCase(speakAnswers = false).invoke(recording).toList()

        assertEquals(VoiceQuestionStep.Answered(assistant.reply, emptyList()), steps.last())
        assertNull(textToSpeech.lastText)
        assertEquals(0, player.plays)
    }

    @Test
    fun emptyTranscription_throwsNothingHeard_withoutAskingTheAssistant() = runTest {
        speechToText.text = "  "

        assertThrows(NothingHeardException::class.java) {
            kotlinx.coroutines.runBlocking { useCase().invoke(recording).toList() }
        }
        assertNull(assistant.lastQuestion)
    }

    @Test
    fun assistantFailure_propagates_withoutSpeaking() = runTest {
        assistant.error = IllegalStateException("boom")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase().invoke(recording).toList() }
        }
        assertEquals(0, player.plays)
    }
}

class FakeSpeechToText : SpeechToText {
    var text = "hola"
    var lastLanguage: String? = null
    override suspend fun transcribe(audio: File, languageCode: String?): Transcription {
        lastLanguage = languageCode
        return Transcription(text)
    }
}

class FakeAssistant : Assistant {
    var reply = "respuesta"
    var data: List<AssistantData> = emptyList()
    var error: Exception? = null
    var lastQuestion: String? = null
    override suspend fun answer(question: String): AssistantAnswer {
        lastQuestion = question
        error?.let { throw it }
        return AssistantAnswer(reply, data)
    }
}

class FakeTextToSpeech : TextToSpeech {
    var lastText: String? = null
    var lastFile: File? = null
    override suspend fun synthesize(text: String): File {
        lastText = text
        return File.createTempFile("tts", ".mp3").also { lastFile = it }
    }
}

class FakePlayer : AudioPlayer {
    var plays = 0
    override suspend fun play(file: File) {
        plays++
    }
}
