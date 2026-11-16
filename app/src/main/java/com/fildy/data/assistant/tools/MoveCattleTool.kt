package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.model.CattleCategory
import com.fildy.data.repository.FarmRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Moves animals of one category from one paddock to another, checking there are enough first. */
class MoveCattleTool @Inject constructor(
    private val farmRepository: FarmRepository,
    private val json: Json,
) : AssistantTool {

    override val name = "move_cattle"

    override val description =
        "Moves animals of one category from one paddock (lote) to another and saves the change. " +
            "Use it when the user asks to move, pass or transfer animals, e.g. \"mové 30 vacas del lote 1 " +
            "al lote 2\". Only call it when the user said the category, how many, and both paddocks."

    override val parameters = objectSchema {
        putEnum(
            "category",
            "Animal category: COW = vacas, HEIFER = vaquillonas, CALF = terneros, STEER = novillos, BULL = toros.",
            CattleCategory.entries.map { it.name },
        )
        putString("from_paddock", "Name of the paddock the animals leave, exactly as listed (e.g. \"Lote 1\").")
        putString("to_paddock", "Name of the paddock the animals go to, exactly as listed (e.g. \"Lote 2\").")
        putInteger("head_count", "How many animals to move. Must be greater than 0.")
    }

    @Serializable
    private data class Arguments(
        val category: String,
        @SerialName("from_paddock") val fromPaddock: String,
        @SerialName("to_paddock") val toPaddock: String,
        @SerialName("head_count") val headCount: Int,
    )

    override suspend fun execute(arguments: String): ToolOutput {
        val args = json.decodeFromString<Arguments>(arguments)
        val category = CattleCategory.entries.firstOrNull { it.name == args.category }
            ?: return error("Unknown category ${args.category}.")
        val overview = farmRepository.observeFarmOverview().first()
            ?: return error("The farm isn't loaded yet.")
        val paddockNames = overview.paddocks.map { it.paddock.name }

        val from = overview.paddocks.findPaddock(args.fromPaddock)
            ?: return error("Unknown paddock '${args.fromPaddock}'.") { putPaddocks(paddockNames) }
        val to = overview.paddocks.findPaddock(args.toPaddock)
            ?: return error("Unknown paddock '${args.toPaddock}'.") { putPaddocks(paddockNames) }
        if (from.paddock.id == to.paddock.id) return error("Source and destination are the same paddock.")
        if (args.headCount <= 0) return error("The number of animals must be greater than 0.")

        val available = from.headCount(category)
        if (available < args.headCount) {
            return error("Not enough animals: ${from.paddock.name} has $available of $category, can't move ${args.headCount}.") {
                put("available", available)
            }
        }

        val move = try {
            farmRepository.moveCattle(category, from.paddock.id, to.paddock.id, args.headCount)
        } catch (e: IllegalArgumentException) {
            // The stock changed between the check and the move.
            return error(e.message ?: "The move was rejected.")
        }
        val remaining = available - args.headCount
        val destinationTotal = to.headCount(category) + args.headCount

        val forModel = buildJsonObject {
            put("moved", true)
            put("category", category.name)
            put("head_count", args.headCount)
            put("from_paddock", from.paddock.name)
            put("to_paddock", to.paddock.name)
            put("remaining_in_source", remaining)
            put("total_in_destination", destinationTotal)
        }.toString().also { Log.i(TAG, "$name → $it") }
        return ToolOutput(
            forModel,
            AssistantData.CattleMoved(move, from.paddock.name, to.paddock.name, remaining, destinationTotal),
        )
    }
}

