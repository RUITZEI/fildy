package com.fildy.data.assistant

import com.fildy.data.assistant.tools.PaddockHerdTool
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.CattleCategory.BULL
import com.fildy.data.model.CattleCategory.CALF
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.Farm
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.Paddock
import com.fildy.data.model.PaddockStock
import com.fildy.ui.home.FakeFarmRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaddockHerdToolTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val repository = FakeFarmRepository().apply {
        overview.value = FarmOverview(
            farm = Farm(1, "La Tranquera", "Castelli", "Buenos Aires"),
            paddocks = listOf(
                stock(1, "Lote 1", COW to 150, CALF to 148, BULL to 50),
                stock(2, "Lote 2"),
            ),
        )
    }
    private val tool = PaddockHerdTool(repository, json)

    private fun stock(id: Long, name: String, vararg counts: Pair<CattleCategory, Int>) = PaddockStock(
        paddock = Paddock(id, 1, name),
        headCounts = CattleCategory.entries.associateWith { c -> counts.toMap()[c] ?: 0 },
    )

    private fun parse(output: ToolOutput): JsonObject = json.parseToJsonElement(output.forModel).jsonObject

    @Test
    fun returnsTotalAndEveryCategory() = runTest {
        val output = tool.execute("""{"paddock":"Lote 1","category":null}""")

        val result = parse(output)
        assertEquals("Lote 1", result["paddock"]!!.jsonPrimitive.content)
        assertEquals(348, result["total"]!!.jsonPrimitive.int)
        val byCategory = result["by_category"]!!.jsonObject
        assertEquals(CattleCategory.entries.map { it.name }.toSet(), byCategory.keys) // zeros included
        assertEquals(150, byCategory["COW"]!!.jsonPrimitive.int)
        assertEquals(0, byCategory["HEIFER"]!!.jsonPrimitive.int)

        val data = output.data as AssistantData.PaddockHerd
        assertEquals("Lote 1", data.paddockName)
        assertEquals(348, data.total)
        assertEquals(150, data.headCounts[COW])
        assertNull(data.category)
        assertFalse("asked_category" in result)
    }

    @Test
    fun withCategory_marksTheAskedCategory() = runTest {
        val output = tool.execute("""{"paddock":"Lote 1","category":"COW"}""")

        val result = parse(output)
        assertEquals("COW", result["asked_category"]!!.jsonPrimitive.content)
        assertEquals(150, result["asked_category_count"]!!.jsonPrimitive.int)
        assertEquals(348, result["total"]!!.jsonPrimitive.int) // context is still there
        assertEquals(COW, (output.data as AssistantData.PaddockHerd).category)
    }

    @Test
    fun withCategoryTheyDontHave_isZero() = runTest {
        val result = parse(tool.execute("""{"paddock":"Lote 1","category":"STEER"}"""))
        assertEquals(0, result["asked_category_count"]!!.jsonPrimitive.int)
    }

    @Test
    fun unknownCategory_isAnError() = runTest {
        val output = tool.execute("""{"paddock":"Lote 1","category":"SHEEP"}""")
        assertTrue("error" in parse(output))
        assertNull(output.data)
    }

    @Test
    fun schema_categoryIsANullableEnum_andStillRequired() {
        val properties = tool.parameters["properties"]!!.jsonObject
        val required = tool.parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(setOf("paddock", "category"), required)

        val category = properties["category"]!!.jsonObject
        assertEquals(listOf("string", "null"), category["type"]!!.jsonArray.map { it.jsonPrimitive.content })
        val values = category["enum"]!!.jsonArray
        assertTrue(values.contains(JsonNull))
        assertEquals(CattleCategory.entries.map { it.name }, values.filterNot { it == JsonNull }.map { it.jsonPrimitive.content })
    }

    @Test
    fun emptyPaddock_hasZeroTotal() = runTest {
        val data = tool.execute("""{"paddock":"Lote 2"}""").data as AssistantData.PaddockHerd
        assertEquals(0, data.total)
    }

    @Test
    fun forgivingPaddockNames() = runTest {
        assertEquals("Lote 1", (tool.execute("""{"paddock":"lote 1"}""").data as AssistantData.PaddockHerd).paddockName)
        assertEquals("Lote 1", (tool.execute("""{"paddock":"1"}""").data as AssistantData.PaddockHerd).paddockName)
    }

    @Test
    fun unknownPaddock_errorListsTheValidOnes() = runTest {
        val output = tool.execute("""{"paddock":"Lote 9"}""")
        val result = parse(output)
        assertTrue("error" in result)
        assertEquals(listOf("Lote 1", "Lote 2"), result["paddocks"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertNull(output.data)
    }
}
