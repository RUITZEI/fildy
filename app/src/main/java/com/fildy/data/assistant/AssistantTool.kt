package com.fildy.data.assistant

import kotlinx.serialization.json.JsonObject

/**
 * A function the assistant can call to read the farm's data. Provider-agnostic: each provider
 * (e.g. OpenAI) turns these into its own function definitions, so adding a tool never touches
 * provider code. Register new tools in `AssistantModule` with `@IntoSet`.
 */
interface AssistantTool {
    /** Unique function name, snake_case, e.g. "get_rainfall_on_date". */
    val name: String

    /** What the tool does and when to use it; the model reads this to decide. */
    val description: String

    /**
     * JSON Schema of the arguments object. Must work in strict mode: every property listed in
     * "required" and "additionalProperties": false.
     */
    val parameters: JsonObject

    /**
     * Runs the tool. [arguments] is the JSON object the model sent. Returns the result as JSON
     * for the model plus the same result typed as [AssistantData] for the app. On bad input,
     * return an `{"error": "..."}` JSON with no data so the model can recover, instead of throwing.
     */
    suspend fun execute(arguments: String): ToolOutput
}
