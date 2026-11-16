package com.fildy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fildy.ui.theme.FildyMotion

/**
 * Shows [content] once [visible] becomes true by sliding it up 40 dp and fading it in
 * (emphasized decelerate, 400 ms). Use [delayMillis] to cascade several cards (40–80 ms apart).
 */
@Composable
fun EnterFromBelow(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val distance = with(LocalDensity.current) { 40.dp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            tween(FildyMotion.EmphasizedDecelerateDuration, delayMillis, FildyMotion.EmphasizedDecelerate),
        ) + slideInVertically(
            tween(FildyMotion.EmphasizedDecelerateDuration, delayMillis, FildyMotion.EmphasizedDecelerate),
        ) { distance },
    ) {
        content()
    }
}
