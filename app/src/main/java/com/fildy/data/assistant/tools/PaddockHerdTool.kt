package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.model.CattleCategory
import com.fildy.data.repository.FarmRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** How many animals a paddock has, in total and per category. */
class PaddockHerdTool @Inject constructor(
    private val farmRepository: FarmRepository,
    private val json: Json,
) : AssistantTool {

    override val name = "get_paddock_herd"

    override val description =
        "Returns how many animals a paddock (lote) has right now: the total and the count per category " +
            "(COW = vacas, HEIFER = vaquillonas, CALF = terneros, STEER = novillos, BULL = toros). Use it " +
            "for questions like \"¿cuántos animales hay en el lote 5?\" or \"¿cuántas vacas hay en el lote 2?\"."

    override val parameters = objectSchema {
        putString("paddock", "Name of the paddock, exactly as listed (e.g. \"Lote 5\").")
        putNullableEnum(
            "category",
            "The category the user asked about (e.g. COW for \"¿cuántas vacas…?\"), or null when they asked " +
                "about all the animals (\"¿cuántos animales…?\").",
            CattleCategory.entries.map { it.name },
        )
    }

    @Serializable
    private data class Arguments(val paddock: String, val category: String? = null)

    override suspend fun execute(arguments: String): ToolOutput {
        val args = json.decodeFromString<Arguments>(arguments)
        val overview = farmRepository.observeFarmOverview().first()
            ?: return error("The farm isn't loaded yet.")
        val stock = overview.paddocks.findPaddock(args.paddock)
            ?: return error("Unknown paddock '${args.paddock}'.") { putPaddocks(overview.paddocks.map { it.paddock.name }) }
        val category = args.category?.let { name ->
            CattleCategory.entries.firstOrNull { it.name == name } ?: return error("Unknown category $name.")
        }

        val forModel = buildJsonObject {
            put("paddock", stock.paddock.name)
            put("total", stock.totalHeadCount)
            if (category != null) {
                put("asked_category", category.name)
                put("asked_category_count", stock.headCount(category))
            }
            putJsonObject("by_category") {
                stock.headCounts.forEach { (category, count) -> put(category.name, count) }
            }
        }.toString().also { Log.d(TAG, "$name(${args.paddock}) → $it") }
        return ToolOutput(forModel, AssistantData.PaddockHerd(stock.paddock.name, stock.headCounts, category))
    }
}
