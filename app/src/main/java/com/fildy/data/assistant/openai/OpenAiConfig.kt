package com.fildy.data.assistant.openai

import com.openai.models.ChatModel

/** Settings for the OpenAI assistant. The key comes from local.properties or an environment variable. */
data class OpenAiConfig(
    val apiKey: String,
    /** The model the assistant uses. */
    val model: ChatModel = ChatModel.GPT_6_LUNA,
    /** Safety limit for function-calling round trips in one answer. */
    val maxToolRounds: Int = 5,
) {
    override fun toString(): String = "OpenAiConfig(model=$model, maxToolRounds=$maxToolRounds)" // never print the key
}
