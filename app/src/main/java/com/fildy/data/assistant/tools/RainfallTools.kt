package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.repository.RainfallRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** How much it rained on one day. */
class RainfallOnDateTool @Inject constructor(
    private val rainfallRepository: RainfallRepository,
    private val json: Json,
) : AssistantTool {

    override val name = "get_rainfall_on_date"

    override val description =
        "Returns the rain recorded for the whole farm on a single day, in millimeters. " +
            "Use it for questions about one specific day (e.g. today, yesterday, last night, a given date)."

    override val parameters = objectSchema {
        putDate("date", "The day to look up, in ISO format YYYY-MM-DD.")
    }

    @Serializable
    private data class Arguments(val date: String)

    override suspend fun execute(arguments: String): ToolOutput {
        val date = parseDate(json.decodeFromString<Arguments>(arguments).date)
            ?: return error("Invalid date, expected YYYY-MM-DD: $arguments")
        val record = rainfallRepository.getRainfall(date)
        val forModel = buildJsonObject {
            put("date", date.toString())
            put("recorded", record != null)
            if (record != null) put("millimeters", record.millimeters)
            else put("note", "No rain recorded for this day.")
        }.toString().also { Log.d(TAG, "$name($date) → $it") }
        return ToolOutput(forModel, AssistantData.RainOnDate(date, record?.millimeters))
    }
}

/** Total and daily rain between two dates. */
class RainfallBetweenDatesTool @Inject constructor(
    private val rainfallRepository: RainfallRepository,
    private val json: Json,
) : AssistantTool {

    override val name = "get_rainfall_between_dates"

    override val description =
        "Returns the rain recorded for the whole farm between two dates (both included): the total in " +
            "millimeters and each day it rained. Days without a record had no rain recorded. Use it for " +
            "periods such as this week, this month, last month or the last N days."

    override val parameters = objectSchema {
        putDate("from", "First day of the period, in ISO format YYYY-MM-DD.")
        putDate("to", "Last day of the period (included), in ISO format YYYY-MM-DD.")
    }

    @Serializable
    private data class Arguments(val from: String, val to: String)

    override suspend fun execute(arguments: String): ToolOutput {
        val args = json.decodeFromString<Arguments>(arguments)
        val from = parseDate(args.from) ?: return error("Invalid 'from' date, expected YYYY-MM-DD: ${args.from}")
        val to = parseDate(args.to) ?: return error("Invalid 'to' date, expected YYYY-MM-DD: ${args.to}")
        if (from > to) return error("'from' ($from) is after 'to' ($to).")
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) return error("Period too long; use at most $MAX_DAYS days.")

        val records = rainfallRepository.getRainfallBetween(from, to).filter { it.millimeters > 0 }
        val total = records.sumOf { it.millimeters }
        val forModel = buildJsonObject {
            put("from", from.toString())
            put("to", to.toString())
            put("total_millimeters", total)
            put("days_with_rain", records.size)
            putJsonArray("rainy_days") {
                records.forEach { record ->
                    addJsonObject {
                        put("date", record.date.toString())
                        put("millimeters", record.millimeters)
                    }
                }
            }
        }.toString().also { Log.d(TAG, "$name($from..$to) → $it") }
        return ToolOutput(forModel, AssistantData.RainBetween(from, to, total, records))
    }

    private companion object {
        const val MAX_DAYS = 366L
    }
}

private const val TAG = "Fildy.AssistantTools"

private fun parseDate(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (e: DateTimeParseException) {
    null
}

/** A failed call: an error the model can read and react to, and no data for the app. */
private fun error(message: String): ToolOutput =
    ToolOutput(buildJsonObject { put("error", message) }.toString().also { Log.w(TAG, message) })

/** JSON Schema for an object whose properties are all required (strict mode). */
private fun objectSchema(properties: PropertiesBuilder.() -> Unit): JsonObject {
    val builder = PropertiesBuilder().apply(properties)
    return buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(builder.properties))
        putJsonArray("required") { builder.properties.keys.forEach { add(it) } }
        put("additionalProperties", false)
    }
}

private class PropertiesBuilder {
    val properties = linkedMapOf<String, JsonObject>()

    fun putDate(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "string")
            put("description", description)
        }
    }
}
