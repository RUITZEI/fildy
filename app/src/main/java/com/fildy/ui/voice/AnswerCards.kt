package com.fildy.ui.voice

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fildy.R
import com.fildy.data.model.CattleCategory
import com.fildy.ui.components.DataCard
import com.fildy.ui.components.RainChart
import com.fildy.ui.components.formatted
import com.fildy.ui.components.rememberAppLocale
import com.fildy.ui.home.farm.labelRes
import com.fildy.ui.theme.Divider
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.Laguna
import com.fildy.ui.theme.Musgo
import com.fildy.ui.theme.Outline
import com.fildy.ui.theme.Tinta
import com.fildy.ui.theme.forestBackground
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shows the card for any [AnswerCard] type. */
@Composable
fun AnswerCardView(card: AnswerCard, modifier: Modifier = Modifier) {
    when (card) {
        is AnswerCard.RainDay -> RainDayCard(card, modifier)
        is AnswerCard.RainPeriod -> RainPeriodCard(card, modifier)
        is AnswerCard.RainfallSaved -> RainfallSavedCard(card, modifier)
        is AnswerCard.PaddockHerd -> PaddockHerdCard(card, modifier)
        is AnswerCard.PaddockCategory -> PaddockCategoryCard(card, modifier)
        is AnswerCard.HerdAdjusted -> HerdAdjustedCard(card, modifier)
        is AnswerCard.CattleMoved -> CattleMovedCard(card, modifier)
    }
}

/** "HACIENDA · LOTE 1", "150 vacas" and "de 348 animales en el lote" for context. */
@Composable
private fun PaddockCategoryCard(card: AnswerCard.PaddockCategory, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    DataCard(modifier) {
        Overline(R.drawable.ic_grid, stringResource(R.string.answer_card_herd, card.paddockName))
        Spacer(Modifier.height(8.dp))
        BigValue(
            value = card.count.formatted(locale),
            unit = pluralStringResource(card.category.nounPluralRes, card.count),
            color = Tinta,
        )
        Text(
            text = pluralStringResource(R.plurals.answer_card_of_total, card.total, card.total.formatted(locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = Musgo,
        )
    }
}

/** "HACIENDA · LOTE 5", the total and how many of each category, as in the design's answer card. */
@Composable
private fun PaddockHerdCard(card: AnswerCard.PaddockHerd, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    DataCard(modifier) {
        Overline(R.drawable.ic_grid, stringResource(R.string.answer_card_herd, card.paddockName))
        Spacer(Modifier.height(8.dp))
        BigValue(
            value = card.total.formatted(locale),
            unit = pluralStringResource(R.plurals.answer_card_animals_unit, card.total),
            color = Tinta,
        )
        card.counts.chunked(3).forEach { row ->
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                row.forEachIndexed { index, (category, count) ->
                    if (index > 0) {
                        Spacer(
                            Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Divider),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = count.formatted(locale),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Tinta,
                        )
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = Musgo,
                            maxLines = 1,
                        )
                    }
                }
                // Keep columns the same width when the last row is shorter.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Receipt for recorded rain: "18 mm el dom 15 nov" and what it replaced, if anything. */
@Composable
private fun RainfallSavedCard(card: AnswerCard.RainfallSaved, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    ReceiptCard(
        icon = R.drawable.ic_droplet,
        overline = stringResource(R.string.answer_card_rain_saved),
        title = stringResource(
            R.string.answer_card_rain_saved_title,
            card.millimeters.formatted(locale),
            card.date.shortLabel(locale, withWeekday = true),
        ),
        detail = card.previousMillimeters
            ?.let { stringResource(R.string.answer_card_rain_saved_replaced, it.formatted(locale)) }
            ?: stringResource(R.string.answer_card_rain_saved_new),
        savedAt = card.savedAt,
        modifier = modifier,
    )
}

/** Receipt for a herd move: "30 vacas del Lote 1 al Lote 2" and the counts after it. */
@Composable
private fun CattleMovedCard(card: AnswerCard.CattleMoved, modifier: Modifier = Modifier) {
    ReceiptCard(
        overline = stringResource(R.string.answer_card_herd_move),
        title = stringResource(
            R.string.answer_card_move,
            pluralStringResource(card.category.countPluralRes, card.headCount, card.headCount),
            card.fromPaddockName,
            card.toPaddockName,
        ),
        detail = stringResource(
            R.string.answer_card_move_counts,
            card.remainingInSource,
            card.fromPaddockName,
            card.totalInDestination,
            card.toPaddockName,
        ),
        savedAt = card.savedAt,
        modifier = modifier,
    )
}

/** Receipt for an adjusted count: "Se agregaron 20 vacas al Lote 1", then "Antes 150 → ahora 170". */
@Composable
private fun HerdAdjustedCard(card: AnswerCard.HerdAdjusted, modifier: Modifier = Modifier) {
    val count = pluralStringResource(card.category.countPluralRes, card.amount, card.amount)
    val title = when (card.kind) {
        AnswerCard.HerdAdjusted.Kind.ADD ->
            pluralStringResource(R.plurals.answer_card_adjust_add, card.amount, count, card.paddockName)
        AnswerCard.HerdAdjusted.Kind.REMOVE ->
            pluralStringResource(R.plurals.answer_card_adjust_remove, card.amount, count, card.paddockName)
        AnswerCard.HerdAdjusted.Kind.SET ->
            stringResource(R.string.answer_card_adjust_set, stringResource(card.category.labelRes), card.paddockName, card.newCount)
    }
    ReceiptCard(
        overline = stringResource(R.string.answer_card_herd_adjust),
        title = title,
        detail = stringResource(R.string.answer_card_adjust_counts, card.previousCount, card.newCount),
        savedAt = card.savedAt,
        modifier = modifier,
    )
}

/**
 * A saved change, like the design's "Comprobante": overline, what was done, a detail line, a
 * dashed cut line and when it was saved.
 */
@Composable
private fun ReceiptCard(
    overline: String,
    title: String,
    detail: String,
    savedAt: Instant,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_grid,
) {
    DataCard(modifier) {
        Overline(icon, overline)
        Spacer(Modifier.height(6.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = Tinta)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Musgo,
            modifier = Modifier.padding(top = 2.dp),
        )
        DashedDivider(Modifier.padding(top = 12.dp, bottom = 8.dp))
        Text(
            text = stringResource(R.string.answer_card_saved_at, savedAt.timeLabel()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Musgo,
        )
    }
}

/** The receipt's 2 dp dashed "cut" line. */
@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawLine(
            color = Outline,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}

/** "19:48" in the device's time zone. */
private fun Instant.timeLabel(): String =
    DateTimeFormatter.ofPattern("HH:mm").format(atZone(ZoneId.systemDefault()))

/** "vaca" / "vacas"… (the noun alone, after a big number). */
private val CattleCategory.nounPluralRes: Int
    get() = when (this) {
        CattleCategory.COW -> R.plurals.cattle_noun_cow
        CattleCategory.HEIFER -> R.plurals.cattle_noun_heifer
        CattleCategory.CALF -> R.plurals.cattle_noun_calf
        CattleCategory.STEER -> R.plurals.cattle_noun_steer
        CattleCategory.BULL -> R.plurals.cattle_noun_bull
    }

/** "30 vacas", "1 toro"… */
private val CattleCategory.countPluralRes: Int
    get() = when (this) {
        CattleCategory.COW -> R.plurals.cattle_count_cow
        CattleCategory.HEIFER -> R.plurals.cattle_count_heifer
        CattleCategory.CALF -> R.plurals.cattle_count_calf
        CattleCategory.STEER -> R.plurals.cattle_count_steer
        CattleCategory.BULL -> R.plurals.cattle_count_bull
    }

/** "LLUVIA · DOM 15 NOV" + the millimeters, or that nothing was recorded. */
@Composable
private fun RainDayCard(card: AnswerCard.RainDay, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    DataCard(modifier) {
        Overline(R.drawable.ic_droplet, stringResource(R.string.answer_card_rain, card.date.shortLabel(locale, withWeekday = true)))
        Spacer(Modifier.height(8.dp))
        val mm = card.millimeters
        if (mm != null) {
            BigValue(mm.formatted(locale), stringResource(R.string.rain_unit))
        } else {
            Text(
                text = stringResource(R.string.answer_card_rain_none),
                style = MaterialTheme.typography.titleLarge,
                color = Tinta,
            )
        }
    }
}

/** "LLUVIA · 1 NOV – 16 NOV" + total, rainy days and a bar per day. */
@Composable
private fun RainPeriodCard(card: AnswerCard.RainPeriod, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    DataCard(modifier) {
        val period = stringResource(
            R.string.answer_card_period,
            card.from.shortLabel(locale, withWeekday = false),
            card.to.shortLabel(locale, withWeekday = false),
        )
        Overline(R.drawable.ic_droplet, stringResource(R.string.answer_card_rain, period))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigValue(card.totalMillimeters.formatted(locale), stringResource(R.string.rain_unit))
            Text(
                text = pluralStringResource(R.plurals.answer_card_rainy_days, card.rainyDayCount, card.rainyDayCount),
                style = MaterialTheme.typography.bodyMedium,
                color = Musgo,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        RainChart(
            millimeters = card.dailyMillimeters,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
        )
    }
}

/** Small uppercase label with an icon, as in the design's answer card ("HACIENDA · LOTE 5"). */
@Composable
private fun Overline(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = Musgo, modifier = Modifier.size(14.dp))
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, color = Musgo)
    }
}

/** A key number, 34 sp, with its unit; Laguna (rain's data color) by default. */
@Composable
private fun BigValue(value: String, unit: String, color: Color = Laguna) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)) {
                append(value)
            }
            withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)) { append(" $unit") }
        },
        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 40.sp),
        color = color,
    )
}

/** "dom 15 nov" / "15 nov" in the app's locale, without trailing dots. */
private fun LocalDate.shortLabel(locale: Locale, withWeekday: Boolean): String =
    DateTimeFormatter.ofPattern(if (withWeekday) "EEE d MMM" else "d MMM", locale).format(this).replace(".", "")

@Preview(widthDp = 360)
@Composable
private fun AnswerCardsPreview() {
    val today = LocalDate.of(2026, 11, 16)
    FildyTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .forestBackground()
                .padding(18.dp),
        ) {
            AnswerCardView(AnswerCard.RainDay(today.minusDays(1), 18.0))
            AnswerCardView(AnswerCard.RainDay(today.minusDays(2), null))
            AnswerCardView(
                AnswerCard.RainPeriod(
                    from = LocalDate.of(2026, 11, 1),
                    to = today,
                    totalMillimeters = 37.0,
                    rainyDayCount = 2,
                    dailyMillimeters = List(16) { i -> when (i) { 6 -> 19.0; 14 -> 18.0; else -> 0.0 } },
                ),
            )
            AnswerCardView(
                AnswerCard.PaddockHerd(
                    paddockName = "Lote 1",
                    total = 348,
                    counts = listOf(CattleCategory.COW to 150, CattleCategory.CALF to 148, CattleCategory.BULL to 50),
                ),
            )
            AnswerCardView(
                AnswerCard.RainfallSaved(
                    date = today.minusDays(1),
                    millimeters = 18.0,
                    previousMillimeters = 12.0,
                    savedAt = Instant.parse("2026-11-16T22:48:00Z"),
                ),
            )
            AnswerCardView(AnswerCard.PaddockCategory("Lote 1", CattleCategory.COW, count = 150, total = 348))
            AnswerCardView(
                AnswerCard.HerdAdjusted(
                    category = CattleCategory.COW,
                    paddockName = "Lote 1",
                    kind = AnswerCard.HerdAdjusted.Kind.ADD,
                    amount = 20,
                    previousCount = 150,
                    newCount = 170,
                    savedAt = Instant.parse("2026-11-16T22:48:00Z"),
                ),
            )
            AnswerCardView(
                AnswerCard.HerdAdjusted(
                    category = CattleCategory.BULL,
                    paddockName = "Lote 8",
                    kind = AnswerCard.HerdAdjusted.Kind.REMOVE,
                    amount = 1,
                    previousCount = 300,
                    newCount = 299,
                    savedAt = Instant.parse("2026-11-16T22:50:00Z"),
                ),
            )
            AnswerCardView(
                AnswerCard.CattleMoved(
                    category = CattleCategory.COW,
                    headCount = 30,
                    fromPaddockName = "Lote 1",
                    toPaddockName = "Lote 2",
                    remainingInSource = 120,
                    totalInDestination = 170,
                    savedAt = Instant.parse("2026-11-16T22:48:00Z"),
                ),
            )
        }
    }
}
