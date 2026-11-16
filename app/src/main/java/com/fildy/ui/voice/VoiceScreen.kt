package com.fildy.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fildy.R
import com.fildy.ui.components.ForestScenery
import com.fildy.ui.components.VoiceOrb
import com.fildy.ui.theme.FildyMotion
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.OnGradientVariant

@Composable
fun VoiceScreen(
    onKeyboardClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VoiceContent(
        uiState = uiState,
        onStartRecording = viewModel::startRecording,
        onStopRecording = viewModel::stopRecording,
        onKeyboardClick = onKeyboardClick,
        onClose = onClose,
        modifier = modifier,
    )
}

// Vertical positions from the design (dp from the top of the safe area).
private val OrbTopIdle = 202.dp
private val OrbSizeIdle = 220.dp
private val OrbTopRecording = 30.dp
private val OrbSizeRecording = 64.dp
private val PromptTop = OrbTopIdle + OrbSizeIdle + 46.dp
private val ListeningLabelTop = OrbTopRecording + OrbSizeRecording + 8.dp

@Composable
fun VoiceContent(
    uiState: VoiceUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onKeyboardClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = uiState.isRecording
    val orbSpec = tween<Dp>(FildyMotion.EmphasizedDuration, easing = FildyMotion.Emphasized)
    val orbTop by animateDpAsState(if (recording) OrbTopRecording else OrbTopIdle, orbSpec, label = "orbTop")
    val orbSize by animateDpAsState(if (recording) OrbSizeRecording else OrbSizeIdle, orbSpec, label = "orbSize")
    val fade = tween<Float>(FildyMotion.StandardDuration, easing = FildyMotion.Standard)

    Box(modifier.fillMaxSize()) {
        ForestScenery(Modifier.fillMaxSize(), showLandscape = false)

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = !recording,
                enter = fadeIn(fade),
                exit = fadeOut(fade),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 30.dp),
            ) {
                Text(
                    text = uiState.placeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnGradientVariant,
                )
            }

            VoiceOrb(
                fastBreathing = recording,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = orbTop)
                    .size(orbSize),
            )

            AnimatedVisibility(
                visible = recording,
                enter = fadeIn(fade),
                exit = fadeOut(fade),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = ListeningLabelTop),
            ) {
                Text(
                    text = stringResource(R.string.voice_listening),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnGradientVariant,
                )
            }

            AnimatedVisibility(
                visible = !recording,
                enter = fadeIn(fade),
                exit = fadeOut(fade),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PromptTop, start = 24.dp, end = 24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.voice_prompt_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, letterSpacing = (-0.3).sp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.voice_prompt_hint),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                        color = OnGradientVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        VoiceControls(
            isRecording = recording,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onKeyboardClick = onKeyboardClick,
            onCloseClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentIdlePreview() {
    FildyTheme {
        VoiceContent(VoiceUiState(placeName = "La Tranquera"), {}, {}, {}, {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentRecordingPreview() {
    FildyTheme {
        VoiceContent(VoiceUiState(placeName = "La Tranquera", isRecording = true), {}, {}, {}, {})
    }
}
