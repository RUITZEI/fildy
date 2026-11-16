package com.fildy.data.assistant

import com.fildy.data.assistant.tools.MoveCattleTool
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.CattleCategory.COW
import com.fildy.data.model.Farm
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.Paddock
import com.fildy.data.model.PaddockStock
import com.fildy.ui.home.FakeFarmRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveCattleToolTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val repository = FakeFarmRepository().apply {
        overview.value = FarmOverview(
            farm = Farm(1, "La Tranquera", "Castelli", "Buenos Aires"),
            paddocks = listOf(
                stock(1, "Lote 1", COW to 150),
                stock(2, "Lote 2", COW to 140),
                stock(3, "Lote 3"),
            ),
        )
    }
    private val tool = MoveCattleTool(repository, json)

    private fun stock(id: Long, name: String, vararg counts: Pair<CattleCategory, Int>) = PaddockStock(
        paddock = Paddock(id, 1, name),
        headCounts = CattleCategory.entries.associateWith { c -> counts.toMap()[c] ?: 0 },
    )

    private fun args(category: String = "COW", from: String = "Lote 1", to: String = "Lote 2", count: Int = 30) =
        """{"category":"$category","from_paddock":"$from","to_paddock":"$to","head_count":$count}"""

    private fun parse(output: ToolOutput): JsonObject = json.parseToJsonElement(output.forModel).jsonObject

    private fun cows(paddockId: Long) =
        repository.overview.value!!.paddocks.first { it.paddock.id == paddockId }.headCount(COW)

    @Test
    fun movesAndReturnsTheReceiptData() = runTest {
        val output = tool.execute(args())

        val result = parse(output)
        assertTrue(result["moved"]!!.jsonPrimitive.boolean)
        assertEquals(120, result["remaining_in_source"]!!.jsonPrimitive.int)
        assertEquals(170, result["total_in_destination"]!!.jsonPrimitive.int)
        assertEquals(120, cows(1))
        assertEquals(170, cows(2))

        val data = output.data as AssistantData.CattleMoved
        assertEquals(repository.moves.single(), data.move)
        assertEquals("Lote 1", data.fromPaddockName)
        assertEquals("Lote 2", data.toPaddockName)
        assertEquals(120, data.remainingInSource)
        assertEquals(170, data.totalInDestination)
    }

    @Test
    fun notEnoughAnimals_errorWithAvailable_andNothingMoves() = runTest {
        val output = tool.execute(args(count = 151))

        val result = parse(output)
        assertTrue("error" in result)
        assertEquals(150, result["available"]!!.jsonPrimitive.int)
        assertNull(output.data)
        assertTrue(repository.moves.isEmpty())
        assertEquals(150, cows(1))
    }

    @Test
    fun categoryWithNoAnimalsInSource_isRejected() = runTest {
        val result = parse(tool.execute(args(from = "Lote 3", count = 1)))
        assertEquals(0, result["available"]!!.jsonPrimitive.int)
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun unknownPaddock_errorListsTheValidOnes() = runTest {
        val result = parse(tool.execute(args(to = "Lote 9")))
        assertTrue("error" in result)
        assertEquals(listOf("Lote 1", "Lote 2", "Lote 3"), result["paddocks"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun sameSourceAndDestination_isRejected() = runTest {
        assertTrue("error" in parse(tool.execute(args(from = "Lote 1", to = "lote 1"))))
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun zeroOrNegativeCount_isRejected() = runTest {
        assertTrue("error" in parse(tool.execute(args(count = 0))))
        assertTrue("error" in parse(tool.execute(args(count = -5))))
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun unknownCategory_isRejected() = runTest {
        assertTrue("error" in parse(tool.execute(args(category = "SHEEP"))))
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun paddockNamesAreForgiving() = runTest {
        val data = tool.execute(args(from = "  lote 1 ", to = "2", count = 10)).data as AssistantData.CattleMoved
        assertEquals("Lote 1", data.fromPaddockName)
        assertEquals("Lote 2", data.toPaddockName)
    }

    @Test
    fun schemaIsStrictModeCompatible_andCategoryIsAnEnum() {
        val properties = tool.parameters["properties"]!!.jsonObject
        val required = tool.parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(properties.keys, required)
        assertEquals(
            CattleCategory.entries.map { it.name },
            properties["category"]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }
}
