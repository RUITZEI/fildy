package com.fildy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fildy.ui.theme.Fence
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.HillBack
import com.fildy.ui.theme.HillFront
import com.fildy.ui.theme.HillMiddle
import com.fildy.ui.theme.TreeCrown
import com.fildy.ui.theme.TreeCrownHighlight
import com.fildy.ui.theme.TreeCrownSmall
import com.fildy.ui.theme.TreeTrunk
import com.fildy.ui.theme.forestBackground

/**
 * Decorative background of the green screens: the forest gradient, two light rays in the
 * top-right corner and, optionally, a small landscape (hills, trees and a fence) along the bottom.
 */
@Composable
fun ForestScenery(modifier: Modifier = Modifier, showLandscape: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "forestScenery")
    // Main ray pulses between 60% and 100% opacity (7 s cycle).
    val rayAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse),
        label = "rayAlpha",
    )
    // Trees sway between -3° and 3° (4 s cycle).
    val treeAngle by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "treeAngle",
    )

    Box(modifier.forestBackground()) {
        LightRay(
            endInset = (-60).dp, top = (-120).dp, width = 120.dp, height = 520.dp,
            startAlpha = 0.22f,
            modifier = Modifier.graphicsLayer { alpha = rayAlpha },
        )
        LightRay(endInset = 40.dp, top = (-140).dp, width = 60.dp, height = 480.dp, startAlpha = 0.16f)
        if (showLandscape) Landscape(treeAngle = { treeAngle })
    }
}

/**
 * A soft beam of light tilted 28°, positioned from the top-right corner. It fades out towards the
 * bottom and along both sides (drawn offscreen and masked, so it looks blurred on every API level).
 */
@Composable
private fun LightRay(
    endInset: Dp,
    top: Dp,
    width: Dp,
    height: Dp,
    startAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val rayWidth = width.toPx()
        val rayHeight = height.toPx()
        val topLeft = Offset(size.width - endInset.toPx() - rayWidth, top.toPx())
        rotate(degrees = 28f, pivot = topLeft + Offset(rayWidth / 2, rayHeight / 2)) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = startAlpha),
                    1f to Color.Transparent,
                    startY = topLeft.y,
                    endY = topLeft.y + rayHeight,
                ),
                topLeft = topLeft,
                size = Size(rayWidth, rayHeight),
            )
            // Soft edges on both sides.
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to Color.Black,
                    1f to Color.Transparent,
                    startX = topLeft.x,
                    endX = topLeft.x + rayWidth,
                ),
                topLeft = topLeft,
                size = Size(rayWidth, rayHeight),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

private const val LANDSCAPE_WIDTH = 360f
private const val LANDSCAPE_HEIGHT = 300f

private val HillFrontPath =
    PathParser().parsePathString("M0 150 C70 118 140 124 210 142 C270 158 320 150 360 132 V300 H0Z").toPath()
private val HillMiddlePath =
    PathParser().parsePathString("M0 200 C80 176 160 186 240 198 C300 207 340 200 360 194 V300 H0Z").toPath()
private val HillBackPath =
    PathParser().parsePathString("M0 250 C90 232 200 240 360 236 V300 H0Z").toPath()
private val FencePath =
    PathParser().parsePathString("M40 128v20M62 124v20M84 122v20M106 122v20M40 132h66M40 140h66M40 132l66 8").toPath()

/** Bottom-center of the tree group: the trees sway around this point. */
private val TreesPivot = Offset(276.25f, 130f)

@Composable
private fun Landscape(treeAngle: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val scale = size.width / LANDSCAPE_WIDTH
        translate(top = size.height - LANDSCAPE_HEIGHT * scale) {
            scale(scale, pivot = Offset.Zero) {
                drawGlow()
                drawPath(HillFrontPath, HillFront)
                drawPath(HillMiddlePath, HillMiddle)
                drawPath(HillBackPath, HillBack)
                rotate(treeAngle(), pivot = TreesPivot) { drawTrees() }
                drawPath(
                    path = FencePath,
                    color = Fence,
                    alpha = 0.85f,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Soft elliptical light-green glow behind the hills. */
private fun DrawScope.drawGlow() {
    val center = Offset(180f, 132f)
    // An elliptical gradient: a circular one squashed vertically (radii 200 × 90).
    scale(scaleX = 1f, scaleY = 90f / 200f, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to Fence.copy(alpha = 0.35f),
                1f to Fence.copy(alpha = 0f),
                center = center,
                radius = 200f,
            ),
            radius = 200f,
            center = center,
        )
    }
}

private fun DrawScope.drawTrees() {
    // Big tree
    drawRoundRect(TreeTrunk, Offset(262f, 104f), Size(4f, 22f), CornerRadius(2f))
    drawCircle(TreeCrown, radius = 15f, center = Offset(264f, 98f))
    drawCircle(TreeCrownHighlight, radius = 7f, center = Offset(258f, 94f), alpha = 0.7f)
    // Small tree
    drawRoundRect(TreeTrunk, Offset(292f, 114f), Size(3f, 16f), CornerRadius(1.5f))
    drawCircle(TreeCrownSmall, radius = 10f, center = Offset(293.5f, 110f))
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ForestSceneryPreview() {
    FildyTheme { ForestScenery(Modifier.fillMaxSize()) }
}
