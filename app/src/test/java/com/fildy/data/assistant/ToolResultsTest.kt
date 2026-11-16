package com.fildy.data.assistant

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ToolResultsTest {

    private val nov14 = AssistantData.RainOnDate(LocalDate.of(2026, 11, 14), null)
    private val nov15 = AssistantData.RainOnDate(LocalDate.of(2026, 11, 15), 18.0)
    private val month = AssistantData.RainBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 16), 0.0, emptyList())

    @Test
    fun keepsTheLastResultPerTool() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", nov14) // e.g. the model got the date wrong…
        results.record("get_rainfall_on_date", nov15) // …and retried

        assertEquals(listOf(nov15), results.all())
    }

    @Test
    fun failedRetry_keepsThePreviousGoodResult() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", nov15)
        results.record("get_rainfall_on_date", null)

        assertEquals(listOf(nov15), results.all())
    }

    @Test
    fun differentTools_orderedByLatestCall() {
        val results = ToolResults()
        results.record("get_rainfall_on_date", nov14)
        results.record("get_rainfall_between_dates", month)
        results.record("get_rainfall_on_date", nov15)

        assertEquals(listOf(month, nov15), results.all())
    }

    @Test
    fun noToolsUsed_noData() {
        assertEquals(emptyList<AssistantData>(), ToolResults().all())
    }
}
