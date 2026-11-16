package com.fildy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fildy.R
import com.fildy.ui.theme.Blanco
import com.fildy.ui.theme.Bosque
import com.fildy.ui.theme.FildyMotion
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.forestBackground

/** The visual states of the voice button. */
enum class VoiceButtonState {
    /** Home screen pill that opens the voice screen. */
    Pill,

    /** Voice screen, ready: big round button with a microphone. */
    Idle,

    /** Voice screen, user speaking: big round button with a stop square. */
    Recording,
}

/**
 * The white voice button. Its size and shape depend on [state]: a 66 dp pill on the home screen,
 * an 88 dp circle on the voice screen. Pass `Modifier.sharedContainer(SharedElementKeys.VoiceButton)`
 * in [modifier] to morph between both across screens.
 */
@Composable
fun VoiceButton(
    state: VoiceButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isPill = state == VoiceButtonState.Pill
    val description = when (state) {
        VoiceButtonState.Pill -> null // The visible label already describes it.
        VoiceButtonState.Idle -> stringResource(R.string.voice_start_description)
        VoiceButtonState.Recording -> stringResource(R.string.voice_stop_description)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (state == VoiceButtonState.Recording || !enabled) Modifier else Modifier.pulsingRing())
            .drawBehind { drawSpread(if (isPill) 6.dp else 8.dp, Color.White.copy(alpha = if (isPill) 0.15f else 0.14f)) }
            .dropShadow(CircleShape, Shadow(radius = 30.dp, color = Color.Black.copy(alpha = 0.25f), offset = DpOffset(0.dp, 10.dp)))
            .clip(CircleShape)
            // Fade only the button itself when disabled: an alpha on the whole modifier chain
            // would clip the shadow and ring to the button's square bounds.
            .alpha(if (enabled) 1f else 0.6f)
            .background(Blanco)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        if (isPill) PillContent() else CircleContent(state)
    }
}

@Composable
private fun PillContent() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .height(66.dp)
            .padding(start = 8.dp, end = 28.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .background(Bosque, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(
            text = stringResource(R.string.voice_tap_to_talk),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 19.sp, lineHeight = 24.sp),
            fontWeight = FontWeight.ExtraBold,
            color = Bosque,
            maxLines = 1,
        )
    }
}

@Composable
private fun CircleContent(state: VoiceButtonState) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            val spec = tween<Float>(FildyMotion.StandardDuration, easing = FildyMotion.Standard)
            (fadeIn(spec) + scaleIn(spec, initialScale = 0.6f)) togetherWith
                (fadeOut(spec) + scaleOut(spec, targetScale = 0.6f))
        },
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(88.dp),
        label = "voiceButtonIcon",
    ) { target ->
        Box(contentAlignment = Alignment.Center) {
            when (target) {
                VoiceButtonState.Recording -> CircleIcon(R.drawable.ic_stop, 30.dp)
                else -> CircleIcon(R.drawable.ic_mic, 36.dp)
            }
        }
    }
}

@Composable
private fun CircleIcon(@DrawableRes icon: Int, size: Dp) {
    Icon(painterResource(icon), contentDescription = null, tint = Bosque, modifier = Modifier.size(size))
}

/** A white ring that keeps expanding (0 → 22 dp) and fading out */
@Composable
private fun Modifier.pulsingRing(): Modifier {
    val transition = rememberInfiniteTransition(label = "pulsingRing")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseOut), RepeatMode.Restart),
        label = "pulse",
    )
    return drawBehind {
        drawSpread(22.dp * progress, Color.White.copy(alpha = 0.35f * (1f - progress)))
    }
}

/** Fills a fully rounded shape [spread] larger than the element on every side. */
private fun DrawScope.drawSpread(spread: Dp, color: Color) {
    val s = spread.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-s, -s),
        size = Size(size.width + 2 * s, size.height + 2 * s),
        cornerRadius = CornerRadius(size.height / 2 + s),
    )
}

@Preview(widthDp = 360, heightDp = 200)
@Composable
private fun VoiceButtonPreview() {
    FildyTheme {
        Box(Modifier.forestBackground()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                modifier = Modifier.padding(36.dp),
            ) {
                VoiceButton(VoiceButtonState.Idle, onClick = {})
                VoiceButton(VoiceButtonState.Recording, onClick = {})
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 140)
@Composable
private fun VoiceButtonPillPreview() {
    FildyTheme {
        Box(Modifier.forestBackground().padding(36.dp)) {
            VoiceButton(VoiceButtonState.Pill, onClick = {})
        }
    }
}
