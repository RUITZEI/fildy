package com.fildy.domain.voice

import android.util.Log
import com.fildy.data.assistant.ConversationTurn
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * The recent exchanges of one voice session, so follow-ups work ("¿Cuántas vacas hay en el
 * lote?" → "¿De qué lote?" → "Del lote uno").
 *
 * Bounded on purpose: keeps the last [MAX_TURNS] exchanges and forgets everything after
 * [EXPIRY] without activity, so an old topic doesn't leak into a new question. Not shared: each
 * voice screen gets its own (it lives with the screen's use case), so leaving the screen clears it.
 */
class ConversationHistory @Inject constructor(private val clock: Clock) {

    private val turns = ArrayDeque<ConversationTurn>()
    private var lastActivity: Instant? = null

    /** The turns to send with the next question, oldest first; empty if the session expired. */
    fun recent(): List<ConversationTurn> {
        val last = lastActivity
        if (last != null && Duration.between(last, clock.instant()) > EXPIRY) {
            Log.d(TAG, "History expired after ${EXPIRY.toMinutes()} min without activity")
            clear()
        }
        return turns.toList()
    }

    fun add(question: String, answer: String) {
        turns.addLast(ConversationTurn(question, answer))
        while (turns.size > MAX_TURNS) turns.removeFirst()
        lastActivity = clock.instant()
    }

    fun clear() {
        turns.clear()
        lastActivity = null
    }

    companion object {
        const val MAX_TURNS = 3
        val EXPIRY: Duration = Duration.ofMinutes(3)
        private const val TAG = "Fildy.Conversation"
    }
}
