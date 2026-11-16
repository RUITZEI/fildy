package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.repository.RainfallRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Records the rain of one day for the whole farm, replacing that day's value if it had one. */
class SetRainfallTool @Inject constructor(
    private val rainfallRepository: RainfallRepository,
    private val json: Json,
    private val clock: Clock,
) : AssistantTool {

    override val name = "set_rainfall"

    override val description =
        "Records how much it rained on one day for the whole farm, in millimeters, and saves it. There's one " +
            "value per day: recording a day that already has rain replaces it. Use it when the user tells you " +
            "the rain (e.g. \"anotá 18 mm de lluvia de ayer\", \"llovieron 25 milímetros hoy\"), not when they ask " +
            "how much it rained."

    override val parameters = objectSchema {
        putDate("date", "The day the rain fell, in ISO format YYYY-MM-DD. Can't be in the future.")
        putNumber("millimeters", "Rain in millimeters, 0 or more.")
    }

    @Serializable
    private data class Arguments(val date: String, val millimeters: Double)

    override suspend fun execute(arguments: String): ToolOutput {
        val args = json.decodeFromString<Arguments>(arguments)
        val date = parseDate(args.date) ?: return error("Invalid date, expected YYYY-MM-DD: ${args.date}")
        val today = LocalDate.now(clock)
        if (date > today) return error("Can't record rain for a future date ($date); today is $today.")
        if (args.millimeters < 0) return error("Rain can't be negative.")

        val update = rainfallRepository.setRainfall(date, args.millimeters)
        val forModel = buildJsonObject {
            put("saved", true)
            put("date", date.toString())
            put("millimeters", update.record.millimeters)
            put("replaced", update.previous != null)
            update.previous?.let { put("previous_millimeters", it.millimeters) }
        }.toString().also { Log.i(TAG, "$name → $it") }
        return ToolOutput(forModel, AssistantData.RainfallSaved(update))
    }
}
