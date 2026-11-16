package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.ToolOutput
import com.fildy.data.model.PaddockStock
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

// Helpers shared by the assistant tools.

internal const val TAG = "Fildy.AssistantTools"

internal fun parseDate(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (e: DateTimeParseException) {
    null
}

/**
 * A failed call: an error the model can read and react to, and no data for the app.
 * [details] adds extra fields (e.g. how many animals are available) to help the model explain.
 */
internal fun error(message: String, details: JsonObjectBuilder.() -> Unit = {}): ToolOutput = ToolOutput(
    buildJsonObject {
        put("error", message)
        details()
    }.toString().also { Log.w(TAG, it) },
)

/**
 * Finds a paddock by name, forgiving case and spacing ("lote 1", "Lote 1"), or by its number
 * alone ("1") when names are like "Lote 1".
 */
internal fun List<PaddockStock>.findPaddock(name: String): PaddockStock? {
    val wanted = name.normalized()
    return firstOrNull { it.paddock.name.normalized() == wanted }
        ?: wanted.toIntOrNull()?.let { number -> firstOrNull { it.paddock.name.number() == number } }
}

private fun String.normalized() = trim().lowercase().replace(Regex("\\s+"), " ")

private fun String.number(): Int? = Regex("\\d+").find(this)?.value?.toIntOrNull()

/** Adds the list of valid paddock names to an error, so the model can correct itself. */
internal fun JsonObjectBuilder.putPaddocks(names: List<String>) =
    putJsonArray("paddocks") { names.forEach { add(it) } }

/** JSON Schema for an object whose properties are all required (strict mode). */
internal fun objectSchema(properties: PropertiesBuilder.() -> Unit): JsonObject {
    val builder = PropertiesBuilder().apply(properties)
    return buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(builder.properties))
        putJsonArray("required") { builder.properties.keys.forEach { add(it) } }
        put("additionalProperties", false)
    }
}

internal class PropertiesBuilder {
    val properties = linkedMapOf<String, JsonObject>()

    fun putDate(name: String, description: String) = putString(name, description)

    fun putString(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "string")
            put("description", description)
        }
    }

    fun putEnum(name: String, description: String, values: List<String>) {
        properties[name] = buildJsonObject {
            put("type", "string")
            put("description", description)
            putJsonArray("enum") { values.forEach { add(it) } }
        }
    }

    /**
     * An optional enum. Strict mode needs every property in "required", so "optional" means the
     * model must send it, but may send null.
     */
    fun putNullableEnum(name: String, description: String, values: List<String>) {
        properties[name] = buildJsonObject {
            putJsonArray("type") {
                add("string")
                add("null")
            }
            put("description", description)
            putJsonArray("enum") {
                values.forEach { add(it) }
                add(JsonNull)
            }
        }
    }

    fun putNumber(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "number")
            put("description", description)
        }
    }

    fun putInteger(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "integer")
            put("description", description)
        }
    }
}
