package com.fildy.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.forestBackground

/** Number of bars in [VoiceWaveform]; also the length of the level history it expects. */
const val WAVEFORM_BAR_COUNT = 24

private val BarWidth = 5.dp
private val BarGap = 4.dp
private val MinBarHeight = 4.dp
private val MaxBarHeight = 30.dp

/** Below this level a bar counts as silence and is drawn faded. */
private const val SILENCE = 0.08f

/**
 * Voice bars that show the microphone is capturing audio. [levels] are loudness values from
 * 0 to 1, oldest first; the newest sample is the rightmost bar. Each bar springs to its new
 * height so the waveform moves smoothly.
 */
@Composable
fun VoiceWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val heights: List<State<Float>> = List(WAVEFORM_BAR_COUNT) { index ->
        // Right-align the history so the newest sample is always the last bar.
        val level = levels.getOrNull(levels.size - WAVEFORM_BAR_COUNT + index) ?: 0f
        animateFloatAsState(
            targetValue = level,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "bar$index",
        )
    }

    Canvas(
        modifier
            .width(BarWidth * WAVEFORM_BAR_COUNT + BarGap * (WAVEFORM_BAR_COUNT - 1))
            .height(MaxBarHeight + 2.dp),
    ) {
        val barWidth = BarWidth.toPx()
        val step = barWidth + BarGap.toPx()
        val minHeight = MinBarHeight.toPx()
        val maxHeight = MaxBarHeight.toPx()
        heights.forEachIndexed { index, state ->
            val level = state.value.coerceIn(0f, 1f)
            val barHeight = minHeight + (maxHeight - minHeight) * level
            drawRoundRect(
                color = Color.White,
                alpha = if (level < SILENCE) 0.35f else 1f,
                topLeft = Offset(index * step, (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}

@Preview
@Composable
private fun VoiceWaveformPreview() {
    // Bar heights from the design (screen 3), as levels.
    val design = listOf(6, 10, 18, 26, 20, 12, 22, 30, 24, 14, 10, 18, 26, 20, 12, 8, 14, 20, 12, 8, 2, 2, 0, 0)
    FildyTheme {
        VoiceWaveform(
            levels = design.map { it / 30f },
            modifier = Modifier
                .forestBackground()
                .padding(24.dp),
        )
    }
}
