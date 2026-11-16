package com.fildy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.fildy.ui.theme.LagunaBar
import com.fildy.ui.theme.LagunaBarEmpty

/**
 * Daily rain bars, one per day in [millimeters], oldest on the left. Rainy days are wider and as
 * tall as their rain relative to the wettest day; dry days are thin 3 dp marks.
 */
@Composable
fun RainChart(millimeters: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (millimeters.isEmpty()) return@Canvas
        val gap = 1.dp.toPx()
        val dryHeight = 3.dp.toPx()
        val radius = CornerRadius(3.dp.toPx())
        val maxRain = millimeters.max().takeIf { it > 0 } ?: 1.0
        // Rainy days take 3 units of width, dry days 1, as in the design.
        val units = millimeters.sumOf { if (it > 0) RAINY_WIDTH else DRY_WIDTH }
        val unitWidth = (size.width - gap * (millimeters.size - 1)) / units

        var x = 0f
        millimeters.forEach { mm ->
            val rainy = mm > 0
            val width = unitWidth * if (rainy) RAINY_WIDTH else DRY_WIDTH
            val height = if (rainy) {
                (size.height * (mm / maxRain)).toFloat().coerceAtLeast(dryHeight * 2)
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
