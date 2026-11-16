package com.fildy.data.assistant.openai

import com.fildy.data.assistant.ConversationTurn
import com.openai.models.responses.EasyInputMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationInputTest {

    @Test
    fun historyBecomesAlternatingMessages_thenTheNewQuestion() {
        val input = conversationInput(
            history = listOf(ConversationTurn("¿Cuántas vacas hay en el lote?", "¿De qué lote?")),
            question = "Del lote uno",
        )

        val messages = input.map { it.asEasyInputMessage() }
        assertEquals(
            listOf(EasyInputMessage.Role.USER, EasyInputMessage.Role.ASSISTANT, EasyInputMessage.Role.USER),
            messages.map { it.role() },
        )
        assertEquals(
            listOf("¿Cuántas vacas hay en el lote?", "¿De qué lote?", "Del lote uno"),
            messages.map { it.content().asTextInput() },
        )
    }

    @Test
    fun noHistory_onlyTheQuestion() {
        val input = conversationInput(emptyList(), "¿Cuánto llovió ayer?")
        assertEquals(listOf("¿Cuánto llovió ayer?"), input.map { it.asEasyInputMessage().content().asTextInput() })
    }
}
