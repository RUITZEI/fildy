package com.fildy.data.assistant

import com.fildy.data.assistant.tools.AdjustHerdTool
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.CattleCategory.BULL
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.Farm
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.model.Paddock
import com.fildy.data.model.PaddockStock
import com.fildy.ui.home.FakeFarmRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdjustHerdToolTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val repository = FakeFarmRepository().apply {
        overview.value = FarmOverview(
            farm = Farm(1, "La Tranquera", "Castelli", "Buenos Aires"),
            paddocks = listOf(stock(1, "Lote 1", COW to 150), stock(8, "Lote 8", BULL to 300)),
        )
    }
    private val tool = AdjustHerdTool(repository, json)

    private fun stock(id: Long, name: String, vararg counts: Pair<CattleCategory, Int>) = PaddockStock(
        paddock = Paddock(id, 1, name),
        headCounts = CattleCategory.entries.associateWith { c -> counts.toMap()[c] ?: 0 },
    )

    private fun args(category: String = "COW", paddock: String = "Lote 1", operation: String, amount: Int) =
        """{"category":"$category","paddock":"$paddock","operation":"$operation","amount":$amount}"""

    private fun parse(output: ToolOutput): JsonObject = json.parseToJsonElement(output.forModel).jsonObject

    private fun count(paddockId: Long, category: CattleCategory) =
        repository.overview.value!!.paddocks.first { it.paddock.id == paddockId }.headCount(category)

    @Test
    fun add() = runTest {
        val output = tool.execute(args(operation = "ADD", amount = 20))

        assertEquals(170, count(1, COW))
        val result = parse(output)
        assertEquals(150, result["previous_count"]!!.jsonPrimitive.int)
        assertEquals(170, result["new_count"]!!.jsonPrimitive.int)
        val data = output.data as AssistantData.HerdAdjusted
        assertEquals(HerdAdjustment.Add(20), data.adjustment)
        assertEquals("Lote 1", data.paddockName)
        assertEquals(repository.adjustments.single(), data.change)
    }

    @Test
    fun remove() = runTest {
        val data = tool.execute(args(category = "BULL", paddock = "lote 8", operation = "REMOVE", amount = 10)).data
            as AssistantData.HerdAdjusted

        assertEquals(290, count(8, BULL))
        assertEquals(HerdAdjustment.Remove(10), data.adjustment)
        assertEquals(300, data.change.previousHeadCount)
    }

    @Test
    fun removeMoreThanAvailable_errorWithAvailable_andNothingChanges() = runTest {
        val output = tool.execute(args(operation = "REMOVE", amount = 151))

        assertEquals(150, parse(output)["available"]!!.jsonPrimitive.int)
        assertNull(output.data)
        assertEquals(150, count(1, COW))
        assertTrue(repository.adjustments.isEmpty())
    }

    @Test
    fun set() = runTest {
        val data = tool.execute(args(operation = "SET", amount = 155)).data as AssistantData.HerdAdjusted
        assertEquals(155, count(1, COW))
        assertEquals(HerdAdjustment.Set(155), data.adjustment)
    }

    @Test
    fun setToZero_isAllowed() = runTest {
        tool.execute(args(operation = "SET", amount = 0))
        assertEquals(0, count(1, COW))
    }

    @Test
    fun invalidAmounts_areRejected() = runTest {
        listOf(args(operation = "ADD", amount = 0), args(operation = "REMOVE", amount = -1), args(operation = "SET", amount = -5))
            .forEach { assertTrue(it, "error" in parse(tool.execute(it))) }
        assertTrue(repository.adjustments.isEmpty())
    }

    @Test
    fun unknownOperationCategoryOrPaddock_areRejected() = runTest {
        assertTrue("error" in parse(tool.execute(args(operation = "MULTIPLY", amount = 2))))
        assertTrue("error" in parse(tool.execute(args(category = "SHEEP", operation = "ADD", amount = 2))))
        val unknownPaddock = parse(tool.execute(args(paddock = "Lote 9", operation = "ADD", amount = 2)))
        assertEquals(listOf("Lote 1", "Lote 8"), unknownPaddock["paddocks"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertTrue(repository.adjustments.isEmpty())
    }

    @Test
    fun schemaIsStrictModeCompatible() {
        val properties = tool.parameters["properties"]!!.jsonObject
        val required = tool.parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(properties.keys, required)
        assertEquals(
            listOf("ADD", "REMOVE", "SET"),
            properties["operation"]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }
}
