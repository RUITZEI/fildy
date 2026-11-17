package com.fildy.data.assistant

import com.fildy.data.model.CattleCategory
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.model.RainfallRecord
import com.fildy.data.model.RainfallUpdate
import com.fildy.data.model.StockChange
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

    /** Rain recorded for a day; [update] keeps the replaced record, if any, for undo. */
    data class RainfallSaved(val update: RainfallUpdate) : AssistantData

    /**
     * Animals in one paddock right now; [headCounts] has every category (0 if none). [category] is
     * the one the user asked about, or null if they asked about all the animals.
     */
    data class PaddockHerd(
        val paddockName: String,
        val headCounts: Map<CattleCategory, Int>,
        val category: CattleCategory? = null,
    ) : AssistantData {
        val total: Int get() = headCounts.values.sum()
    }

    /**
     * The count of one category in a paddock was adjusted ([adjustment]: add, remove or set).
     * [change] is the saved correction (previous → new count); it can be undone with
     * `FarmRepository.undo`.
     */
    data class HerdAdjusted(
        val change: StockChange.Correction,
        val paddockName: String,
        val adjustment: HerdAdjustment,
    ) : AssistantData

    /**
     * Animals moved between paddocks. [move] is the saved change (it can be undone with
     * `FarmRepository.undo`); the counts are for the moved category after the move.
     */
    data class CattleMoved(
        val move: StockChange.Move,
        val fromPaddockName: String,
        val toPaddockName: String,
        val remainingInSource: Int,
        val totalInDestination: Int,
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
 * Collects the data of the tools called while answering, to show as cards.
 *
 * A tool that succeeded more than once in the same answer gets no card: one card can't stand for
 * several results (e.g. the model looping over every paddock to total the farm would otherwise
 * show only the last paddock, which contradicts the answer). Failed calls don't count, so an
 * error followed by a corrected call still gets its card. Cards are in the order tools were first
 * called.
 */
class ToolResults {
    private val byTool = LinkedHashMap<String, MutableList<AssistantData>>()

    fun record(toolName: String, data: AssistantData?) {
        if (data == null) return // failed calls don't produce or block a card
        byTool.getOrPut(toolName) { mutableListOf() } += data
    }

    /** One entry per tool that succeeded exactly once. */
    fun all(): List<AssistantData> = byTool.values.mapNotNull { it.singleOrNull() }
}
