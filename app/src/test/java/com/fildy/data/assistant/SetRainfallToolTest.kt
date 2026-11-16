package com.fildy.data.assistant

import com.fildy.data.assistant.tools.SetRainfallTool
import com.fildy.data.model.RainfallRecord
import com.fildy.ui.home.FakeRainfallRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SetRainfallToolTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    private val repository = FakeRainfallRepository().apply {
        records.value = listOf(
            RainfallRecord(id = 1, farmId = 1, date = LocalDate.of(2026, 11, 15), millimeters = 12.0, recordedAt = Instant.EPOCH),
        )
    }
    private val tool = SetRainfallTool(repository, json, clock)

    private fun parse(output: ToolOutput): JsonObject = json.parseToJsonElement(output.forModel).jsonObject

    @Test
    fun newDay_isRecorded() = runTest {
        val output = tool.execute("""{"date":"2026-11-16","millimeters":25}""")

        val result = parse(output)
        assertTrue(result["saved"]!!.jsonPrimitive.boolean)
        assertFalse(result["replaced"]!!.jsonPrimitive.boolean)
        val data = output.data as AssistantData.RainfallSaved
        assertEquals(25.0, data.update.record.millimeters, 0.0)
        assertNull(data.update.previous)
        assertEquals(25.0, repository.getRainfall(LocalDate.of(2026, 11, 16))!!.millimeters, 0.0)
    }

    @Test
    fun existingDay_isReplaced_andTheModelIsTold() = runTest {
        val output = tool.execute("""{"date":"2026-11-15","millimeters":18.5}""")

        val result = parse(output)
        assertTrue(result["replaced"]!!.jsonPrimitive.boolean)
        assertEquals(12.0, result["previous_millimeters"]!!.jsonPrimitive.double, 0.0)
        val data = output.data as AssistantData.RainfallSaved
        assertEquals(12.0, data.update.previous!!.millimeters, 0.0)
        assertEquals(1, repository.records.value.count { it.date == LocalDate.of(2026, 11, 15) })
    }

    @Test
    fun futureDate_isRejected() = runTest {
        val output = tool.execute("""{"date":"2026-11-17","millimeters":10}""")
        assertTrue("error" in parse(output))
        assertNull(output.data)
        assertNull(repository.getRainfall(LocalDate.of(2026, 11, 17)))
    }

    @Test
    fun negativeOrInvalid_isRejected() = runTest {
        assertTrue("error" in parse(tool.execute("""{"date":"2026-11-16","millimeters":-3}""")))
        assertTrue("error" in parse(tool.execute("""{"date":"ayer","millimeters":3}""")))
        assertNull(repository.getRainfall(LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun zero_isAllowed() = runTest {
        assertTrue(parse(tool.execute("""{"date":"2026-11-16","millimeters":0}"""))["saved"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun schemaIsStrictModeCompatible() {
        val properties = tool.parameters["properties"]!!.jsonObject
        val required = tool.parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(setOf("date", "millimeters"), required)
        assertEquals(properties.keys, required)
        assertEquals("number", properties["millimeters"]!!.jsonObject["type"]!!.jsonPrimitive.content)
    }
}
