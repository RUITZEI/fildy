package com.fildy.data.assistant

import com.fildy.data.model.RainfallRecord
import java.time.LocalDate

/**
 * Structured data behind an answer, produced by the tools from the farm's own records (never
 * written by the model). The UI turns each type into a card.
 */
sealed interface AssistantData {
    /** Rain on one day; [millimeters] is null when nothing was recorded. */
    data class RainOnDate(val date: LocalDate, val millimeters: Double?) : AssistantData

    /** Rain between two dates (both included); [rainyDays] only has days with rain, oldest first. */
    data class RainBetween(
        val from: LocalDate,
        val to: LocalDate,
        val totalMillimeters: Double,
        val rainyDays: List<RainfallRecord>,
    ) : AssistantData
}

/**
 * What a tool returns: [forModel] is the JSON the model reads; [data] is the same result, typed,
 * for the app. [data] is null when the tool failed (e.g. invalid arguments).
 */
data class ToolOutput(val forModel: String, val data: AssistantData? = null)

/** The assistant's reply: the [text] to show and speak, and the [data] behind it. */
data class AssistantAnswer(val text: String, val data: List<AssistantData> = emptyList())

/**
 * Collects the data of the tools called while answering, keeping only the last successful result
 * per tool: if the model retries a tool (e.g. corrects a date), the earlier result is dropped.
 * Results are ordered by when they were last produced.
 */
class ToolResults {
    private val byTool = LinkedHashMap<String, AssistantData>()

    fun record(toolName: String, data: AssistantData?) {
        if (data == null) return // failed calls don't replace a previous good result
        byTool.remove(toolName) // re-insert so the order reflects the latest call
        byTool[toolName] = data
    }

    fun all(): List<AssistantData> = byTool.values.toList()
}
