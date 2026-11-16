package com.fildy.ui.components

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.fildy.ui.theme.FildyMotion

/** The [SharedTransitionScope] wrapping the app's NavDisplay; null outside it (e.g. previews). */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Shared element keys used across destinations. */
object SharedElementKeys {
    const val VoiceButton = "voice_button"
}

private val EmphasizedBoundsTransform = BoundsTransform { _, _ ->
    tween(FildyMotion.EmphasizedDuration, easing = FildyMotion.Emphasized)
}

/**
 * Container transform between destinations: the element's bounds (size, position and shape via
 * the clip) morph into the matching element on the other screen while their contents crossfade.
 * No-op when there's no shared transition scope (previews).
 */
@Composable
fun Modifier.sharedContainer(key: Any): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(sharedScope) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = EmphasizedBoundsTransform,
            resizeMode = RemeasureToBounds,
        )
    }
}
