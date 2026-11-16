package com.fildy.domain.voice

/**
 * Switches for the voice flow.
 *
 * @param speakAnswers read the assistant's answer out loud with text-to-speech. Off saves
 * text-to-speech credits while testing. Set with SPEAK_ANSWERS in local.properties / environment.
 */
data class VoiceAssistantSettings(val speakAnswers: Boolean)
