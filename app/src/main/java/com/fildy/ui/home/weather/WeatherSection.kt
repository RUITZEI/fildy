package com.fildy.ui.home.weather

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fildy.R
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.OnGradientVariant
import com.fildy.ui.theme.SunEdge
import com.fildy.ui.theme.SunGlow
import com.fildy.ui.theme.SunHighlight
import com.fildy.ui.theme.SunMid
import com.fildy.ui.theme.forestBackground
import com.fildy.ui.theme.glass
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Current weather shown at the top of the home screen. Meant to sit on the forest gradient. */
@Composable
fun WeatherSection(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeatherSection(uiState = uiState, modifier = modifier)
}

@Composable
fun WeatherSection(uiState: WeatherUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CurrentConditions(uiState)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeatherStat(
                icon = R.drawable.ic_droplet,
                label = R.string.weather_humidity,
                value = stringResource(R.string.weather_percent, uiState.humidityPercent),
                modifier = Modifier.weight(1f),
            )
            WeatherStat(
                icon = R.drawable.ic_umbrella,
                label = R.string.weather_rain,
                value = stringResource(R.string.weather_percent, uiState.rainChancePercent),
                modifier = Modifier.weight(1f),
            )
            val windUnit = stringResource(R.string.weather_wind_unit)
            WeatherStat(
                icon = R.drawable.ic_wind,
                label = R.string.weather_wind,
                value = buildAnnotatedString {
                    append(uiState.windSpeedKmh.toString())
                    withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)) {
                        append(" $windUnit")
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CurrentConditions(uiState: WeatherUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (uiState.condition) {
            WeatherCondition.CLEAR -> SunOrb(Modifier.size(62.dp))
        }
        Text(
            text = stringResource(R.string.weather_temperature, uiState.temperatureCelsius),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
        )
        Column {
            Text(
                text = stringResource(uiState.condition.labelRes),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                color = Color.White,
            )
            Text(
                text = stringResource(
                    R.string.weather_date_range,
                    uiState.date.toShortLabel(),
                    uiState.maxTemperatureCelsius,
                    uiState.minTemperatureCelsius,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OnGradientVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeatherStat(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    value: String,
    modifier: Modifier = Modifier,
) = WeatherStat(icon, label, buildAnnotatedString { append(value) }, modifier)

@Composable
private fun WeatherStat(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    value: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .glass(RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = OnGradientVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodySmall,
                color = OnGradientVariant,
                maxLines = 1,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 26.sp),
            color = Color.White,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/** A glowing sun drawn with gradients; the halo slowly pulses. */
@Composable
private fun SunOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sun")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunPulse",
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        // Halo extends 14dp beyond the orb's box and pulses between scale 1 and 1.12.
        val haloRadius = (size.minDimension / 2 + 14.dp.toPx()) * (1f + 0.12f * pulse)
        drawCircle(
            brush = Brush.radialGradient(
                0f to SunGlow.copy(alpha = 0.55f),
                0.7f to SunGlow.copy(alpha = 0f),
                center = center,
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = center,
            alpha = 0.7f + 0.3f * pulse,
        )
        // The sun itself is inset 8dp, lit from the upper-left.
        val sunRadius = size.minDimension / 2 - 8.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                0f to SunHighlight,
                0.55f to SunMid,
                1f to SunEdge,
                center = Offset(center.x - sunRadius * 0.3f, center.y - sunRadius * 0.4f),
                radius = sunRadius * 2f,
            ),
            radius = sunRadius,
            center = center,
        )
    }
}

private val WeatherCondition.labelRes: Int
    @StringRes get() = when (this) {
        WeatherCondition.CLEAR -> R.string.weather_condition_clear
    }

/**
 * "Dom 15 nov" — short weekday, day and month without trailing dots. Formatted in the locale of
 * the strings being shown (R.string.locale_tag) so the date matches the rest of the UI.
 */
@Composable
private fun LocalDate.toShortLabel(): String {
    val locale: Locale = Locale.forLanguageTag(stringResource(R.string.locale_tag))
    return remember(this, locale) {
        DateTimeFormatter.ofPattern("EEE d MMM", locale)
            .format(this)
            .replace(".", "")
            .replaceFirstChar { it.titlecase(locale) }
    }
}

@Preview(widthDp = 360)
@Composable
private fun WeatherSectionPreview() {
    FildyTheme {
        WeatherSection(
            uiState = WeatherUiState(
                temperatureCelsius = 24,
                condition = WeatherCondition.CLEAR,
                date = LocalDate.of(2026, 11, 15),
                maxTemperatureCelsius = 27,
                minTemperatureCelsius = 14,
                humidityPercent = 62,
                rainChancePercent = 10,
                windSpeedKmh = 14,
            ),
            modifier = Modifier
                .forestBackground()
                .padding(18.dp),
        )
    }
}
