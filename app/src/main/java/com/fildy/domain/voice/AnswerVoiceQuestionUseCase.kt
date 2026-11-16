package com.fildy.domain.voice

import android.util.Log
import com.fildy.data.assistant.Assistant
import com.fildy.data.assistant.AssistantData
import com.fildy.data.audio.AudioPlayer
import com.fildy.data.speech.SpeechToText
import com.fildy.data.speech.TextToSpeech
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Progress of answering a spoken question, in order. */
sealed interface VoiceQuestionStep {
    data object Transcribing : VoiceQuestionStep

    /** What the user said. */
    data class Heard(val question: String) : VoiceQuestionStep

    data object Thinking : VoiceQuestionStep

    /** The answer's [text] and the typed [data] behind it (one entry per tool used). */
    data class Answered(val text: String, val data: List<AssistantData>) : VoiceQuestionStep

    /** Reading the answer out loud (only when [VoiceAssistantSettings.speakAnswers]). */
    data object Speaking : VoiceQuestionStep
}

/** The recording had no recognizable speech. */
class NothingHeardException : Exception("Transcription came back empty")

/**
 * Answers a recorded question: speech-to-text → assistant (with tools) → text-to-speech. Keeps
 * the session's recent exchanges in [history] so follow-up questions have context.
 *
 * Emits each [VoiceQuestionStep] as it starts so the screen can follow along, and completes once
 * the answer has been spoken (or right after [VoiceQuestionStep.Answered] when speaking is off).
 * Failures are thrown: [NothingHeardException], or the provider's exception. The caller owns
 * [recording] and should delete it afterwards.
 */
class AnswerVoiceQuestionUseCase @Inject constructor(
    private val speechToText: SpeechToText,
    private val assistant: Assistant,
    private val textToSpeech: TextToSpeech,
    private val player: AudioPlayer,
    private val settings: VoiceAssistantSettings,
    private val history: ConversationHistory,
) {
    operator fun invoke(recording: File): Flow<VoiceQuestionStep> = flow {
        emit(VoiceQuestionStep.Transcribing)
        val question = speechToText.transcribe(recording, languageCode = LANGUAGE).text
        if (question.isBlank()) throw NothingHeardException()
        Log.d(TAG, "Heard: \"$question\"")
        emit(VoiceQuestionStep.Heard(question))

        emit(VoiceQuestionStep.Thinking)
        val answer = assistant.answer(question, history.recent())
        history.add(question, answer.text)
        emit(VoiceQuestionStep.Answered(answer.text, answer.data))

        if (settings.speakAnswers) {
            emit(VoiceQuestionStep.Speaking)
            val speech = textToSpeech.synthesize(answer.text)
            try {
                player.play(speech)
            } finally {
                speech.delete()
            }
        } else {
            Log.d(TAG, "SPEAK_ANSWERS is off: not reading the answer out loud")
        }
    }

    private companion object {
        const val TAG = "Fildy.VoiceQuestion"
        const val LANGUAGE = "es"
    }
}
