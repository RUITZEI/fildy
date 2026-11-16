package com.fildy.ui.voice

import com.fildy.data.assistant.AssistantData
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.HerdAdjustment
import java.time.Instant
import java.time.LocalDate

/** A card shown under the assistant's answer, built from the data its tools returned. */
sealed interface AnswerCard {
    /** Rain on one day; [millimeters] is null when nothing was recorded. */
    data class RainDay(val date: LocalDate, val millimeters: Double?) : AnswerCard

    data class RainPeriod(
        val from: LocalDate,
        val to: LocalDate,
        val totalMillimeters: Double,
        val rainyDayCount: Int,
        /** One value per day from [from] to [to], 0 on dry days, for the bar chart. */
        val dailyMillimeters: List<Double>,
    ) : AnswerCard

    /** Receipt for rain recorded on [date]; [previousMillimeters] is what it replaced, if anything. */
    data class RainfallSaved(
        val date: LocalDate,
        val millimeters: Double,
        val previousMillimeters: Double?,
        val savedAt: Instant,
    ) : AnswerCard

    /** Animals in a paddock: the [total] and the categories it has animals of, in category order. */
    data class PaddockHerd(
        val paddockName: String,
        val total: Int,
        val counts: List<Pair<CattleCategory, Int>>,
    ) : AnswerCard

    /** How many animals of one [category] a paddock has, out of its [total]. */
    data class PaddockCategory(
        val paddockName: String,
        val category: CattleCategory,
        val count: Int,
        val total: Int,
    ) : AnswerCard

    /**
     * Receipt for a count adjusted in one paddock: [kind] says how, [amount] is how many were
     * added/removed (or the total set); counts are for [category] before and after.
     */
    data class HerdAdjusted(
        val category: CattleCategory,
        val paddockName: String,
        val kind: Kind,
        val amount: Int,
        val previousCount: Int,
        val newCount: Int,
        val savedAt: Instant,
    ) : AnswerCard {
        enum class Kind { ADD, REMOVE, SET }
    }

    /** Receipt for animals moved between paddocks; counts are for [category] after the move. */
    data class CattleMoved(
        val category: CattleCategory,
        val headCount: Int,
        val fromPaddockName: String,
        val toPaddockName: String,
        val remainingInSource: Int,
        val totalInDestination: Int,
        val savedAt: Instant,
    ) : AnswerCard
}

fun AssistantData.toAnswerCard(): AnswerCard = when (this) {
    is AssistantData.RainOnDate -> AnswerCard.RainDay(date, millimeters)
    is AssistantData.RainBetween -> {
        val byDate = rainyDays.associate { it.date to it.millimeters }
        AnswerCard.RainPeriod(
            from = from,
            to = to,
            totalMillimeters = totalMillimeters,
            rainyDayCount = rainyDays.size,
            dailyMillimeters = generateSequence(from) { it.plusDays(1) }
                .takeWhile { it <= to }
                .map { byDate[it] ?: 0.0 }
                .toList(),
        )
    }
    is AssistantData.RainfallSaved -> AnswerCard.RainfallSaved(
        date = update.record.date,
        millimeters = update.record.millimeters,
        previousMillimeters = update.previous?.millimeters,
        savedAt = update.record.recordedAt,
    )
    is AssistantData.PaddockHerd -> when (val asked = category) {
        // "¿Cuántos animales hay…?": the total and every category.
        null -> AnswerCard.PaddockHerd(
            paddockName = paddockName,
            total = total,
            counts = CattleCategory.entries.mapNotNull { category ->
                headCounts[category]?.takeIf { it > 0 }?.let { category to it }
            },
        )
        // "¿Cuántas vacas hay…?": only that category.
        else -> AnswerCard.PaddockCategory(
            paddockName = paddockName,
            category = asked,
            count = headCounts[asked] ?: 0,
            total = total,
        )
    }
    is AssistantData.HerdAdjusted -> AnswerCard.HerdAdjusted(
        category = change.category,
        paddockName = paddockName,
        kind = when (adjustment) {
            is HerdAdjustment.Add -> AnswerCard.HerdAdjusted.Kind.ADD
            is HerdAdjustment.Remove -> AnswerCard.HerdAdjusted.Kind.REMOVE
            is HerdAdjustment.Set -> AnswerCard.HerdAdjusted.Kind.SET
        },
        amount = when (adjustment) {
            is HerdAdjustment.Add -> adjustment.headCount
            is HerdAdjustment.Remove -> adjustment.headCount
            is HerdAdjustment.Set -> adjustment.headCount
        },
        previousCount = change.previousHeadCount,
        newCount = change.newHeadCount,
        savedAt = change.recordedAt,
    )
    is AssistantData.CattleMoved -> AnswerCard.CattleMoved(
        category = move.category,
        headCount = move.headCount,
        fromPaddockName = fromPaddockName,
        toPaddockName = toPaddockName,
        remainingInSource = remainingInSource,
        totalInDestination = totalInDestination,
        savedAt = move.recordedAt,
    )
}
