package com.fildy.data.assistant

import com.fildy.data.assistant.tools.RainfallBetweenDatesTool
import com.fildy.data.assistant.tools.RainfallOnDateTool
import com.fildy.data.model.RainfallRecord
import com.fildy.ui.home.FakeRainfallRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RainfallToolsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val repository = FakeRainfallRepository().apply {
        records.value = listOf(
            record("2026-11-01", 12.0),
            record("2026-11-07", 19.0),
            record("2026-11-15", 18.0),
            record("2026-10-31", 9.0),
        )
    }
    private val onDate = RainfallOnDateTool(repository, json)
    private val between = RainfallBetweenDatesTool(repository, json)

    private fun record(date: String, mm: Double) =
        RainfallRecord(id = 0, farmId = 1, date = LocalDate.parse(date), millimeters = mm, recordedAt = Instant.EPOCH)

    private fun parse(output: ToolOutput): JsonObject = json.parseToJsonElement(output.forModel).jsonObject

    @Test
    fun onDate_recordedDay() = runTest {
        val output = onDate.execute("""{"date":"2026-11-15"}""")
        val result = parse(output)
        assertTrue(result["recorded"]!!.jsonPrimitive.boolean)
        assertEquals(18.0, result["millimeters"]!!.jsonPrimitive.double, 0.0)
        assertEquals(AssistantData.RainOnDate(LocalDate.of(2026, 11, 15), 18.0), output.data)
    }

    @Test
    fun onDate_dayWithoutRecord() = runTest {
        val output = onDate.execute("""{"date":"2026-11-14"}""")
        val result = parse(output)
        assertFalse(result["recorded"]!!.jsonPrimitive.boolean)
        assertFalse("millimeters" in result)
        assertEquals(AssistantData.RainOnDate(LocalDate.of(2026, 11, 14), null), output.data)
    }

    @Test
    fun onDate_invalidDate_returnsErrorInsteadOfThrowing() = runTest {
        val output = onDate.execute("""{"date":"ayer"}""")
        assertTrue("error" in parse(output))
        assertNull(output.data)
    }

    @Test
    fun between_thisMonth() = runTest {
        val output = between.execute("""{"from":"2026-11-01","to":"2026-11-16"}""")
        val result = parse(output)
        assertEquals(49.0, result["total_millimeters"]!!.jsonPrimitive.double, 0.0)
        assertEquals(3, result["days_with_rain"]!!.jsonPrimitive.int)
        assertEquals("2026-11-01", result["rainy_days"]!!.jsonArray.first().jsonObject["date"]!!.jsonPrimitive.content)

        val data = output.data as AssistantData.RainBetween
        assertEquals(LocalDate.of(2026, 11, 1), data.from)
        assertEquals(LocalDate.of(2026, 11, 16), data.to)
        assertEquals(49.0, data.totalMillimeters, 0.0)
        assertEquals(listOf(12.0, 19.0, 18.0), data.rainyDays.map { it.millimeters })
    }

    @Test
    fun between_invalidRanges_returnErrors() = runTest {
        assertNull(between.execute("""{"from":"2026-11-16","to":"2026-11-01"}""").data)
        assertTrue("error" in parse(between.execute("""{"from":"2026-11-16","to":"2026-11-01"}""")))
        assertTrue("error" in parse(between.execute("""{"from":"2024-01-01","to":"2026-11-01"}""")))
        assertTrue("error" in parse(between.execute("""{"from":"x","to":"2026-11-01"}""")))
    }

    @Test
    fun schemasAreStrictModeCompatible() {
        listOf(onDate, between).forEach { tool ->
            val properties = tool.parameters["properties"]!!.jsonObject.keys
            val required = tool.parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
            assertEquals(properties, required)
            assertFalse(tool.parameters["additionalProperties"]!!.jsonPrimitive.boolean)
        }
    }
}
