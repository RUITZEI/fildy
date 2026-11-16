package com.fildy.ui.voice

import com.fildy.data.assistant.AssistantData
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.HerdAdjustment
import com.fildy.data.model.RainfallRecord
import com.fildy.data.model.StockChange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AnswerCardTest {

    @Test
    fun rainOnDate_becomesRainDay() {
        val date = LocalDate.of(2026, 11, 15)
        assertEquals(AnswerCard.RainDay(date, 18.0), AssistantData.RainOnDate(date, 18.0).toAnswerCard())
        assertEquals(AnswerCard.RainDay(date, null), AssistantData.RainOnDate(date, null).toAnswerCard())
    }

    @Test
    fun rainBetween_becomesRainPeriod_withOneBarPerDay() {
        val from = LocalDate.of(2026, 11, 1)
        val to = LocalDate.of(2026, 11, 7)
        val data = AssistantData.RainBetween(
            from = from,
            to = to,
            totalMillimeters = 30.0,
            rainyDays = listOf(record(from, 12.0), record(to, 18.0)),
        )

        val card = data.toAnswerCard() as AnswerCard.RainPeriod

        assertEquals(30.0, card.totalMillimeters, 0.0)
        assertEquals(2, card.rainyDayCount)
        assertEquals(listOf(12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 18.0), card.dailyMillimeters)
    }

    private fun record(date: LocalDate, mm: Double) =
        RainfallRecord(id = 0, farmId = 1, date = date, millimeters = mm, recordedAt = Instant.EPOCH)

    @Test
    fun cattleMoved_becomesAReceipt() {
        val move = StockChange.Move(
            id = 7,
            category = CattleCategory.COW,
            recordedAt = Instant.parse("2026-11-16T22:48:00Z"),
            fromPaddockId = 1,
            toPaddockId = 2,
            headCount = 30,
        )
        val card = AssistantData.CattleMoved(move, "Lote 1", "Lote 2", 120, 170).toAnswerCard()

        assertEquals(
            AnswerCard.CattleMoved(
                category = CattleCategory.COW,
                headCount = 30,
                fromPaddockName = "Lote 1",
                toPaddockName = "Lote 2",
                remainingInSource = 120,
                totalInDestination = 170,
                savedAt = Instant.parse("2026-11-16T22:48:00Z"),
            ),
            card,
        )
    }

    @Test
    fun paddockHerd_keepsOnlyCategoriesWithAnimals_inCategoryOrder() {
        val data = AssistantData.PaddockHerd(
            paddockName = "Lote 1",
            headCounts = mapOf(
                CattleCategory.BULL to 50,
                CattleCategory.COW to 150,
                CattleCategory.HEIFER to 0,
                CattleCategory.CALF to 148,
                CattleCategory.STEER to 0,
            ),
        )

        assertEquals(
            AnswerCard.PaddockHerd(
                paddockName = "Lote 1",
                total = 348,
                counts = listOf(CattleCategory.COW to 150, CattleCategory.CALF to 148, CattleCategory.BULL to 50),
            ),
            data.toAnswerCard(),
        )
    }

    @Test
    fun paddockHerdWithCategory_becomesAFocusedCard() {
        val data = AssistantData.PaddockHerd(
            paddockName = "Lote 1",
            headCounts = mapOf(CattleCategory.COW to 150, CattleCategory.CALF to 148, CattleCategory.BULL to 50),
            category = CattleCategory.COW,
        )

        assertEquals(AnswerCard.PaddockCategory("Lote 1", CattleCategory.COW, count = 150, total = 348), data.toAnswerCard())
    }

    @Test
    fun herdAdjusted_becomesAReceipt() {
        val change = StockChange.Correction(
            id = 3,
            category = CattleCategory.BULL,
            recordedAt = Instant.parse("2026-11-16T22:50:00Z"),
            paddockId = 8,
            previousHeadCount = 300,
            newHeadCount = 290,
        )
        val card = AssistantData.HerdAdjusted(change, "Lote 8", HerdAdjustment.Remove(10)).toAnswerCard()

        assertEquals(
            AnswerCard.HerdAdjusted(
                category = CattleCategory.BULL,
                paddockName = "Lote 8",
                kind = AnswerCard.HerdAdjusted.Kind.REMOVE,
                amount = 10,
                previousCount = 300,
                newCount = 290,
                savedAt = Instant.parse("2026-11-16T22:50:00Z"),
            ),
            card,
        )
    }
}
