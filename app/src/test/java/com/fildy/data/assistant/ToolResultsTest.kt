package com.fildy.data.assistant

import com.fildy.data.model.CattleCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ToolResultsTest {

    private val nov14 = AssistantData.RainOnDate(LocalDate.of(2026, 11, 14), null)
    private val nov15 = AssistantData.RainOnDate(LocalDate.of(2026, 11, 15), 18.0)
    private val month = AssistantData.RainBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 16), 0.0, emptyList())

    private fun paddock(name: String) =
        AssistantData.PaddockHerd(name, CattleCategory.entries.associateWith { 10 })

    @Test
    fun oneResult_oneCard() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", nov15)

        assertEquals(listOf(nov15), results.all())
    }

    @Test
    fun toolCalledForEveryPaddock_noCard() {
        // "¿Cuántos animales hay en el campo?" answered by calling the paddock tool 8 times.
        val results = ToolResults()
        (1..8).forEach { results.record("get_paddock_herd", paddock("Lote $it")) }

        assertEquals(emptyList<AssistantData>(), results.all())
    }

    @Test
    fun twoSuccessfulResults_noCard() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", nov14)
        results.record("get_rainfall_on_date", nov15)

        assertEquals(emptyList<AssistantData>(), results.all())
    }

    @Test
    fun failedCallThenCorrectedCall_keepsTheCard() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", null) // e.g. invalid date
        results.record("get_rainfall_on_date", nov15)

        assertEquals(listOf(nov15), results.all())
    }

    @Test
    fun goodResultThenFailedCall_keepsTheCard() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", nov15)
        results.record("get_rainfall_on_date", null)

        assertEquals(listOf(nov15), results.all())
    }

    @Test
    fun onlyTheToolWithSeveralResultsLosesItsCard() {
        val results = ToolResults()
        results.record("get_rainfall_between_dates", month)
        results.record("get_paddock_herd", paddock("Lote 1"))
        results.record("get_paddock_herd", paddock("Lote 2"))
        results.record("get_rainfall_on_date", nov15)

        assertEquals(listOf(month, nov15), results.all())
    }

    @Test
    fun noToolsUsed_noData() {
        assertEquals(emptyList<AssistantData>(), ToolResults().all())
    }
}
