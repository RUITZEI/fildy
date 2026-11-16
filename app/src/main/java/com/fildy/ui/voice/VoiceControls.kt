package com.fildy.ui.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fildy.R
import com.fildy.ui.components.GlassIconButton
import com.fildy.ui.components.SharedElementKeys
import com.fildy.ui.components.VoiceButton
import com.fildy.ui.components.VoiceButtonState
import com.fildy.ui.components.sharedContainer
import com.fildy.ui.theme.FildyMotion

/** Bottom controls of the voice screen: keyboard · voice button · close, with a hint below. */
@Composable
fun VoiceControls(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onKeyboardClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp),
        ) {
            GlassIconButton(
                icon = R.drawable.ic_keyboard,
                contentDescription = stringResource(R.string.voice_keyboard_description),
                onClick = onKeyboardClick,
            )
            VoiceButton(
                state = if (isRecording) VoiceButtonState.Recording else VoiceButtonState.Idle,
                onClick = if (isRecording) onStopRecording else onStartRecording,
                modifier = Modifier.sharedContainer(SharedElementKeys.VoiceButton),
            )
            GlassIconButton(
                icon = R.drawable.ic_close,
                contentDescription = stringResource(R.string.voice_close_description),
                onClick = onCloseClick,
            )
        }
        AnimatedContent(
            targetState = isRecording,
            transitionSpec = {
                val spec = tween<Float>(FildyMotion.StandardDuration, easing = FildyMotion.Standard)
                fadeIn(spec) togetherWith fadeOut(spec)
            },
            label = "voiceHint",
        ) { recording ->
            Text(
                text = stringResource(if (recording) R.string.voice_tap_to_finish else R.string.voice_tap_to_talk),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
    }
}
