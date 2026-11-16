package com.fildy.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fildy.ui.theme.Brote
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.OrbBlobAccent
import com.fildy.ui.theme.OrbBlobDark
import com.fildy.ui.theme.OrbBlobLight
import com.fildy.ui.theme.OrbCore
import com.fildy.ui.theme.OrbEdge
import com.fildy.ui.theme.OrbMid
import com.fildy.ui.theme.forestBackground

/**
 * The living voice sphere: a green orb with soft blobs drifting inside it and a halo around it.
 * It always breathes (scales 1 → 1.06); [fastBreathing] speeds it up while the user is speaking.
 *
 * The orb fills this composable's size. The halo extends 22% beyond it, so leave room around it.
 */
@Composable
fun VoiceOrb(modifier: Modifier = Modifier, fastBreathing: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "voiceOrb")
    val breath by transition.loop(if (fastBreathing) 550 else 1600, "breath")
    val blob1 by transition.loop(3000, "blob1")
    val blob2 by transition.loop(3750, "blob2")
    val blob3 by transition.loop(2500, "blob3")

    Canvas(modifier) {
        val diameter = size.minDimension
        val center = Offset(size.width / 2, size.height / 2)
        scale(1f + 0.06f * breath, pivot = center) {
            drawHalo(center, diameter)
            drawBody(center, diameter, blob1, blob2, blob3)
        }
    }
}

/** A 0 → 1 → 0 value looping forever with ease-in-out; [halfPeriodMs] is half a cycle. */
@Composable
private fun InfiniteTransition.loop(halfPeriodMs: Int, label: String): State<Float> = animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(halfPeriodMs, easing = EaseInOut), RepeatMode.Reverse),
    label = label,
)

private fun DrawScope.drawHalo(center: Offset, diameter: Float) {
    val radius = diameter * 0.72f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Brote.copy(alpha = 0.45f),
            0.68f to Brote.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawBody(center: Offset, d: Float, blob1: Float, blob2: Float, blob3: Float) {
    val radius = d / 2
    val origin = Offset(center.x - radius, center.y - radius) // top-left of the orb

    // Base: radial gradient lit from slightly below the center.
    drawCircle(
        brush = Brush.radialGradient(
            0f to OrbCore,
            0.45f to OrbMid,
            1f to OrbEdge,
            center = origin + Offset(d * 0.5f, d * 0.6f),
            radius = d * 0.78f,
        ),
        radius = radius,
        center = center,
    )

    // Drifting blobs, clipped to the orb. Positions/sizes are fractions of the diameter.
    val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius)) }
    clipPath(clip) {
        translate(origin.x, origin.y) {
            softEllipse(
                color = OrbBlobLight, alpha = 0.9f,
                center = Offset(0.325f + 0.187f * blob1, 0.65f - 0.126f * blob1) * d,
                radiusX = 0.425f * d, radiusY = 0.35f * d, blur = 0.09f * d,
            )
            softEllipse(
                color = OrbBlobDark, alpha = 0.75f,
                center = Offset(0.775f - 0.15f * blob2, 0.275f + 0.143f * blob2) * d,
                radiusX = 0.375f * d, radiusY = 0.325f * d, blur = 0.1f * d,
            )
            val growth = 1f + 0.15f * blob3
            softEllipse(
                color = OrbBlobAccent, alpha = 1f,
                center = Offset(0.6f - 0.084f * blob3, 0.95f - 0.06f * blob3) * d,
                radiusX = 0.35f * d * growth, radiusY = 0.3f * d * growth, blur = 0.08f * d,
            )
            // Glossy highlight, top-left, tilted.
            val highlightCenter = Offset(0.37f, 0.21f) * d
            rotate(-25f, pivot = highlightCenter) {
                softEllipse(
                    color = Color.White, alpha = 0.75f,
                    center = highlightCenter,
                    radiusX = 0.19f * d, radiusY = 0.11f * d, blur = 0.035f * d,
                )
            }
        }
        // Inner glow along the edge.
        drawCircle(
            brush = Brush.radialGradient(
                (1f - 0.24f) to Color.White.copy(alpha = 0f),
                1f to Color.White.copy(alpha = 0.6f),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

    // Thin light outline just outside the orb.
    val outline = d * 0.012f
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = radius + outline / 2,
        center = center,
        style = Stroke(width = outline),
    )
}

/**
 * An ellipse with feathered edges — a cheap stand-in for a CSS blur that works on every API
 * level: solid inside, fading out over [blur] around the edge.
 */
private fun DrawScope.softEllipse(
    color: Color,
    alpha: Float,
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    blur: Float,
) {
    val outer = radiusX + blur
    val solidUntil = ((radiusX - blur) / outer).coerceAtLeast(0f)
    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                solidUntil to color,
                1f to color.copy(alpha = 0f),
                center = center,
                radius = outer,
            ),
            radius = outer,
            center = center,
            alpha = alpha,
        )
    }
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun VoiceOrbPreview() {
    FildyTheme {
        Row(
            modifier = Modifier.forestBackground().padding(40.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            VoiceOrb(Modifier.size(160.dp))
            VoiceOrb(Modifier.size(64.dp), fastBreathing = true)
        }
    }
}
