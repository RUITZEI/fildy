package com.fildy.ui.voice

import com.fildy.data.assistant.AssistantData
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
}
