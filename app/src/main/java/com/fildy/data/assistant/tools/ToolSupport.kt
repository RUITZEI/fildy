package com.fildy.data.assistant.tools

import android.util.Log
import com.fildy.data.assistant.ToolOutput
import java.time.LocalDate
import java.time.format.DateTimeParseException
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

    fun putInteger(name: String, description: String) {
        properties[name] = buildJsonObject {
            put("type", "integer")
            put("description", description)
        }
    }
}
