package com.fildy.data.assistant.openai

import android.util.Log
import com.fildy.data.assistant.Assistant
import com.fildy.data.assistant.AssistantAnswer
import com.fildy.data.assistant.AssistantException
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.SystemPrompt
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.assistant.ToolResults
import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.core.JsonValue
import com.openai.models.responses.EasyInputMessage
import com.openai.models.responses.FunctionTool
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseInputItem
import java.time.Duration
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * [Assistant] backed by the OpenAI Responses API with function calling.
 *
 * Flow: send the question with the instructions and tool definitions → if the model asks for
 * tools, run them and send their results back (chained with `previous_response_id`, which also
 * carries any reasoning between rounds) → repeat until the model answers with text.
 */
class OpenAiAssistant @Inject constructor(
    private val config: OpenAiConfig,
    private val systemPrompt: SystemPrompt,
    tools: Set<@JvmSuppressWildcards AssistantTool>,
) : Assistant {

    private val toolsByName: Map<String, AssistantTool> = tools.associateBy { it.name }
    private val functionTools: List<FunctionTool> = tools.map { it.toFunctionTool() }

    /** Created on first use, so a missing key fails a request instead of app startup. */
    private val client: OpenAIClientAsync by lazy {
        OpenAIOkHttpClientAsync.builder()
            .apiKey(config.apiKey)
            .timeout(Duration.ofSeconds(60))
            .build()
    }

    override suspend fun answer(question: String): AssistantAnswer {
        if (config.apiKey.isBlank()) throw AssistantException("OPENAI_API_KEY is not set (local.properties or environment)")
        Log.d(TAG, "Question: \"$question\" (model=${config.model}, tools=${toolsByName.keys})")

        val instructions = systemPrompt.build()
        var input: List<ResponseInputItem> = listOf(userMessage(question))
        var previousResponseId: String? = null
        val toolResults = ToolResults()

        repeat(config.maxToolRounds + 1) { round ->
            val response = request(instructions, input, previousResponseId, round)
            val calls = response.output().mapNotNull { it.functionCall().getOrNull() }
            if (calls.isEmpty()) {
                val answer = response.answerText()
                if (answer.isBlank()) throw AssistantException("The model returned no text (status=${response.status().getOrNull()})")
                val data = toolResults.all()
                Log.i(TAG, "Answer: \"$answer\" with data: $data")
                return AssistantAnswer(answer, data)
            }
            // Run every tool the model asked for: the JSON goes back to the model in the next
            // round, the typed data is kept for the app.
            input = calls.map { call ->
                val output = runTool(call)
                toolResults.record(call.name(), output.data)
                functionCallOutput(call, output.forModel)
            }
            previousResponseId = response.id()
        }
        throw AssistantException("Gave up after ${config.maxToolRounds} tool rounds")
    }

    private suspend fun request(
        instructions: String,
        input: List<ResponseInputItem>,
        previousResponseId: String?,
        round: Int,
    ): Response {
        val params = ResponseCreateParams.builder()
            .model(config.model)
            .instructions(instructions)
            .inputOfResponse(input)
            .apply { functionTools.forEach { addTool(it) } }
            .apply { previousResponseId?.let { previousResponseId(it) } }
            .build()
        val (response, elapsed) = try {
            measureTimedValue { client.responses().create(params).await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AssistantException("OpenAI request failed: ${e.message}", e)
        }
        val usage = response.usage().getOrNull()
        Log.d(
            TAG,
            "Round $round: ${elapsed.inWholeMilliseconds} ms, id=${response.id()}, " +
                "tokens in=${usage?.inputTokens()} out=${usage?.outputTokens()}",
        )
        return response
    }

    private suspend fun runTool(call: ResponseFunctionToolCall): ToolOutput {
        val tool = toolsByName[call.name()]
            ?: return ToolOutput("""{"error":"Unknown tool ${call.name()}"}""").also {
                Log.w(TAG, "Model called unknown tool ${call.name()}")
            }
        Log.i(TAG, "Tool call: ${call.name()}(${call.arguments()})")
        return try {
            tool.execute(call.arguments())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tool ${call.name()} failed", e)
            ToolOutput("""{"error":"The tool failed: ${e.message?.replace("\"", "'")}"}""")
        }.also { Log.d(TAG, "Tool result: ${it.forModel}") }
    }

    private companion object {
        const val TAG = "Fildy.Assistant"
    }
}

private fun userMessage(text: String): ResponseInputItem = ResponseInputItem.ofEasyInputMessage(
    EasyInputMessage.builder().role(EasyInputMessage.Role.USER).content(text).build(),
)

private fun functionCallOutput(call: ResponseFunctionToolCall, output: String): ResponseInputItem =
    ResponseInputItem.ofFunctionCallOutput(
        ResponseInputItem.FunctionCallOutput.builder().callId(call.callId()).output(output).build(),
    )

/** All text parts of the model's message(s), joined. */
private fun Response.answerText(): String = output()
    .mapNotNull { it.message().getOrNull() }
    .flatMap { message -> message.content().mapNotNull { it.outputText().getOrNull()?.text() } }
    .joinToString("")
    .trim()

private fun AssistantTool.toFunctionTool(): FunctionTool = FunctionTool.builder()
    .name(name)
    .description(description)
    .parameters(
        FunctionTool.Parameters.builder()
            .additionalProperties(parameters.mapValues { (_, value) -> JsonValue.from(value.toPlain()) })
            .build(),
    )
    .strict(true)
    .build()

/** kotlinx JSON → plain Kotlin values (Map/List/String/Number/Boolean/null) for the SDK's JsonValue. */
private fun JsonElement.toPlain(): Any? = when (this) {
    is JsonNull -> null
    is JsonObject -> mapValues { it.value.toPlain() }
    is JsonArray -> map { it.toPlain() }
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        longOrNull != null -> longOrNull
        else -> doubleOrNull
    }
}
