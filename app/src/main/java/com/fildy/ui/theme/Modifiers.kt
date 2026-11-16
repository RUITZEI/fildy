package com.fildy.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Background of the home and voice screens: a vertical forest gradient with a soft light
 * coming from the top-right corner.
 */
fun Modifier.forestBackground(): Modifier = drawBehind {
    drawRect(
        Brush.verticalGradient(
            0.00f to GradientTop,
            0.30f to GradientUpperMid,
            0.62f to GradientLowerMid,
            1.00f to GradientBottom,
        ),
    )
    val radius = maxOf(size.width * 1.2f, size.height * 0.6f)
    drawRect(
        Brush.radialGradient(
            0.00f to GradientLight.copy(alpha = 0.35f),
            0.55f to GradientLight.copy(alpha = 0f),
            center = Offset(size.width * 0.85f, 0f),
            radius = radius,
        ),
    )
}

/** "Glass" surface for content on top of the gradient: white 14% fill + white 18% border. */
fun Modifier.glass(shape: Shape): Modifier = this
    .background(Color.White.copy(alpha = 0.14f), shape)
    .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
