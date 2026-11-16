package com.fildy.ui.home.rain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fildy.R
import com.fildy.ui.components.DataCard
import com.fildy.ui.components.DataCardHeader
import com.fildy.ui.components.EnterFromBelow
import com.fildy.ui.components.formatted
import com.fildy.ui.components.rememberAppLocale
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.Laguna
import com.fildy.ui.theme.LagunaBar
import com.fildy.ui.theme.LagunaBarEmpty
import com.fildy.ui.theme.LagunaClaro
import com.fildy.ui.theme.forestBackground
import java.time.LocalDate

/**
 * "Lluvia · 60 días": total rain, the latest rain and a bar per day. It slides in once the data
 * is loaded, after [enterDelayMillis].
 */
@Composable
fun RainCard(
    modifier: Modifier = Modifier,
    enterDelayMillis: Int = 0,
    viewModel: RainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = uiState as? RainUiState.Success
    EnterFromBelow(visible = loaded != null, modifier = modifier, delayMillis = enterDelayMillis) {
        loaded?.let { RainCard(it) }
    }
}

@Composable
fun RainCard(state: RainUiState.Success, modifier: Modifier = Modifier) {
    val locale = rememberAppLocale()
    val last = state.lastRain
    val subtitle = when {
        last == null -> stringResource(R.string.rain_card_none)
        last.daysAgo == 0L -> stringResource(R.string.rain_card_last_today, last.millimeters.formatted(locale))
        last.daysAgo == 1L -> stringResource(R.string.rain_card_last_yesterday, last.millimeters.formatted(locale))
        else -> pluralStringResource(
            R.plurals.rain_card_last_days_ago,
            last.daysAgo.toInt(),
            last.millimeters.formatted(locale),
            last.daysAgo.toInt(),
        )
    }
    DataCard(modifier) {
        DataCardHeader(
            icon = R.drawable.ic_droplet,
            iconTint = Laguna,
            iconBackground = LagunaClaro,
            title = stringResource(R.string.rain_card_title, state.periodDays),
            subtitle = subtitle,
            value = state.totalMillimeters.formatted(locale),
            unit = stringResource(R.string.rain_unit),
            valueColor = Laguna,
            unitColor = Laguna,
        )
        Spacer(Modifier.height(12.dp))
        val chartDescription = stringResource(R.string.rain_card_chart_description, state.periodDays)
        RainChart(
            days = state.days,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .semantics { contentDescription = chartDescription },
        )
    }
}

/**
 * One bar per day, oldest on the left. Rainy days are wider and as tall as their rain relative
 * to the wettest day; dry days are thin 3 dp marks.
 */
@Composable
private fun RainChart(days: List<DailyRain>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (days.isEmpty()) return@Canvas
        val gap = 1.dp.toPx()
        val dryHeight = 3.dp.toPx()
        val radius = CornerRadius(3.dp.toPx())
        val maxRain = days.maxOf { it.millimeters }.takeIf { it > 0 } ?: 1.0
        // Rainy days take 3 units of width, dry days 1, as in the design.
        val units = days.sumOf { if (it.millimeters > 0) RAINY_WIDTH else DRY_WIDTH }
        val unitWidth = (size.width - gap * (days.size - 1)) / units

        var x = 0f
        days.forEach { day ->
            val rainy = day.millimeters > 0
            val width = unitWidth * if (rainy) RAINY_WIDTH else DRY_WIDTH
            val height = if (rainy) {
                (size.height * (day.millimeters / maxRain)).toFloat().coerceAtLeast(dryHeight * 2)
            } else {
                dryHeight
            }
            drawRoundRect(
                color = if (rainy) LagunaBar else LagunaBarEmpty,
                topLeft = Offset(x, size.height - height),
                size = Size(width, height),
                cornerRadius = radius,
            )
            x += width + gap
        }
    }
}

private const val RAINY_WIDTH = 3
private const val DRY_WIDTH = 1

@Preview(widthDp = 360)
@Composable
private fun RainCardPreview() {
    val today = LocalDate.of(2026, 11, 16)
    val rain = mapOf(56 to 24.0, 47 to 11.0, 39 to 21.0, 30 to 6.0, 23 to 23.0, 16 to 9.0, 9 to 19.0, 1 to 18.0)
    FildyTheme {
        RainCard(
            state = RainUiState.Success(
                periodDays = 60,
                totalMillimeters = 131.0,
                lastRain = LastRain(18.0, daysAgo = 1),
                days = (59 downTo 0).map { DailyRain(today.minusDays(it.toLong()), rain[it] ?: 0.0) },
            ),
            modifier = Modifier
                .forestBackground()
                .padding(18.dp),
        )
    }
}
