package com.fildy.ui.voice

import androidx.annotation.DrawableRes
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
import com.fildy.ui.components.DataCard
import com.fildy.ui.components.RainChart
import com.fildy.ui.components.formatted
import com.fildy.ui.components.rememberAppLocale
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.Laguna
import com.fildy.ui.theme.Musgo
import com.fildy.ui.theme.Tinta
import com.fildy.ui.theme.forestBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shows the card for any [AnswerCard] type. */
@Composable
fun AnswerCardView(card: AnswerCard, modifier: Modifier = Modifier) {
    when (card) {
        is AnswerCard.RainDay -> RainDayCard(card, modifier)
        is AnswerCard.RainPeriod -> RainPeriodCard(card, modifier)
    }
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

/** A key number in Laguna (rain's data color), 34 sp, with its unit. */
@Composable
private fun BigValue(value: String, unit: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)) {
                append(value)
            }
            withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)) { append(" $unit") }
        },
        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 40.sp),
        color = Laguna,
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
        }
    }
}
