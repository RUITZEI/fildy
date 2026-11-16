package com.fildy.domain.voice

import com.fildy.data.assistant.ConversationTurn
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class ConversationHistoryTest {

    private val clock = MutableClock()
    private val history = ConversationHistory(clock)

    @Test
    fun keepsTheLastThreeExchanges_oldestFirst() {
        (1..5).forEach { history.add("q$it", "a$it") }

        assertEquals(
            listOf(ConversationTurn("q3", "a3"), ConversationTurn("q4", "a4"), ConversationTurn("q5", "a5")),
            history.recent(),
        )
    }

    @Test
    fun forgetsAfterThreeMinutesWithoutActivity() {
        history.add("¿Cuántas vacas hay en el lote?", "¿De qué lote?")

        clock.advance(Duration.ofMinutes(3))
        assertEquals(1, history.recent().size) // exactly 3 minutes: still there

        clock.advance(Duration.ofSeconds(1))
        assertEquals(emptyList<ConversationTurn>(), history.recent())
    }

    @Test
    fun eachNewExchangeRestartsTheTimer() {
        history.add("q1", "a1")
        clock.advance(Duration.ofMinutes(2))
        history.add("q2", "a2")
        clock.advance(Duration.ofMinutes(2))

        assertEquals(2, history.recent().size)
    }

    @Test
    fun clear() {
        history.add("q1", "a1")
        history.clear()
        assertEquals(emptyList<ConversationTurn>(), history.recent())
    }
}
