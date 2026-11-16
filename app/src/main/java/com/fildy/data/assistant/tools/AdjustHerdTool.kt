package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.repository.FarmRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Adds, removes or sets the count of one category in a paddock, without moving animals to
 * another paddock (that's [MoveCattleTool]).
 */
class AdjustHerdTool @Inject constructor(
    private val farmRepository: FarmRepository,
    private val json: Json,
) : AssistantTool {

    override val name = "adjust_herd"

    override val description =
        "Changes how many animals of one category a paddock (lote) has and saves the change, without moving " +
            "them to another paddock. operation: ADD for new animals (births, purchases, \"agregá 20 vacas al " +
            "lote 1\"), REMOVE for animals that left the farm (sales, deaths, \"sacá 10 toros del lote 8\"), SET " +
            "when the user gives the real total (\"en el lote 3 hay 155 vacas\"). To move animals between " +
            "paddocks use move_cattle instead. Only call it when the user said the category, the paddock and " +
            "the amount."

    override val parameters = objectSchema {
        putEnum(
            "category",
            "Animal category: COW = vacas, HEIFER = vaquillonas, CALF = terneros, STEER = novillos, BULL = toros.",
            CattleCategory.entries.map { it.name },
        )
        putString("paddock", "Name of the paddock, exactly as listed (e.g. \"Lote 1\").")
        putEnum("operation", "ADD, REMOVE or SET (see the tool description).", Operation.entries.map { it.name })
        putInteger("amount", "ADD/REMOVE: how many animals (greater than 0). SET: the new total (0 or more).")
    }

    private enum class Operation { ADD, REMOVE, SET }

    @Serializable
    private data class Arguments(
        val category: String,
        val paddock: String,
        val operation: String,
        val amount: Int,
    )

    override suspend fun execute(arguments: String): ToolOutput {
        val args = json.decodeFromString<Arguments>(arguments)
        val category = CattleCategory.entries.firstOrNull { it.name == args.category }
            ?: return error("Unknown category ${args.category}.")
        val operation = Operation.entries.firstOrNull { it.name == args.operation }
            ?: return error("Unknown operation ${args.operation}; use ADD, REMOVE or SET.")
        val overview = farmRepository.observeFarmOverview().first()
            ?: return error("The farm isn't loaded yet.")
        val stock = overview.paddocks.findPaddock(args.paddock)
            ?: return error("Unknown paddock '${args.paddock}'.") { putPaddocks(overview.paddocks.map { it.paddock.name }) }

        val current = stock.headCount(category)
        val adjustment = when (operation) {
            Operation.ADD -> {
                if (args.amount <= 0) return error("The number of animals to add must be greater than 0.")
                HerdAdjustment.Add(args.amount)
            }
            Operation.REMOVE -> {
                if (args.amount <= 0) return error("The number of animals to remove must be greater than 0.")
                if (args.amount > current) {
                    return error("Not enough animals: ${stock.paddock.name} has $current of $category, can't remove ${args.amount}.") {
                        put("available", current)
                    }
                }
                HerdAdjustment.Remove(args.amount)
            }
            Operation.SET -> {
                if (args.amount < 0) return error("The total can't be negative.")
                HerdAdjustment.Set(args.amount)
            }
        }

        val change = try {
            farmRepository.adjustHeadCount(stock.paddock.id, category, adjustment)
        } catch (e: IllegalArgumentException) {
            // The stock changed between the check and the adjustment.
            return error(e.message ?: "The change was rejected.")
        }
        val forModel = buildJsonObject {
            put("saved", true)
            put("category", category.name)
            put("paddock", stock.paddock.name)
            put("operation", operation.name)
            put("amount", args.amount)
            put("previous_count", change.previousHeadCount)
            put("new_count", change.newHeadCount)
        }.toString().also { Log.i(TAG, "$name → $it") }
        return ToolOutput(forModel, AssistantData.HerdAdjusted(change, stock.paddock.name, adjustment))
    }
}
