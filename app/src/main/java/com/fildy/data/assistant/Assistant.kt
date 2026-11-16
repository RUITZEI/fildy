package com.fildy.data.assistant

/** Answers the user's questions about the farm, using [AssistantTool]s to read its data. */
interface Assistant {
    /**
     * Answers [question] (a single, standalone question: no conversation history). The answer
     * includes the typed data of the tools used, for the app to show as cards.
     * @throws AssistantException if the provider fails or isn't configured.
     */
    suspend fun answer(question: String): AssistantAnswer
}

class AssistantException(message: String, cause: Throwable? = null) : Exception(message, cause)
