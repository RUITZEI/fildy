package com.fildy.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current

    // TODO: Implement the proper permission requester with a cleaner UI later.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i(TAG, "RECORD_AUDIO permission ${if (granted) "granted" else "denied"}")
        if (granted) viewModel.startRecording() else viewModel.onMicrophonePermissionDenied()
    }

    VoiceContent(
        uiState = uiState,
        onStartRecording = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onStopRecording = viewModel::stopRecording,
        onKeyboardClick = onKeyboardClick,
        onClose = onClose,
        modifier = modifier,
    )
}

private const val TAG = "Fildy.Voice"

// Vertical positions from the design (dp from the top of the safe area).
private val OrbTopIdle = 202.dp
private val OrbSizeIdle = 220.dp
private val OrbTopCompact = 30.dp
private val OrbSizeCompact = 64.dp
private val PromptTop = OrbTopIdle + OrbSizeIdle + 46.dp
private val StatusLabelTop = OrbTopCompact + OrbSizeCompact + 8.dp
private val TranscriptTop = StatusLabelTop + 130.dp

@Composable
fun VoiceContent(
    uiState: VoiceUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onKeyboardClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = uiState.isCompact
    val orbSpec = tween<Dp>(FildyMotion.EmphasizedDuration, easing = FildyMotion.Emphasized)
    val orbTop by animateDpAsState(if (compact) OrbTopCompact else OrbTopIdle, orbSpec, label = "orbTop")
    val orbSize by animateDpAsState(if (compact) OrbSizeCompact else OrbSizeIdle, orbSpec, label = "orbSize")
    val fade = tween<Float>(FildyMotion.StandardDuration, easing = FildyMotion.Standard)

    Box(modifier.fillMaxSize()) {
        ForestScenery(Modifier.fillMaxSize(), showLandscape = false)

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = !compact,
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
                fastBreathing = uiState.phase == VoicePhase.Recording || uiState.phase == VoicePhase.Speaking,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = orbTop)
                    .size(orbSize),
            )

            // Status under the small sphere: Escuchando / Procesando… / Respondiendo.
            AnimatedContent(
                targetState = uiState.phase.statusLabel(),
                transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = StatusLabelTop),
                label = "voiceStatus",
            ) { label ->
                if (label != null) {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnGradientVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = !compact,
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

            // For now, just echoing what the user said, or what went wrong.
            AnimatedContent(
                targetState = uiState.transcript to uiState.errorMessage,
                transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TranscriptTop, start = 24.dp, end = 24.dp),
                label = "voiceTranscript",
            ) { (transcript, error) ->
                when {
                    error != null -> Text(
                        text = stringResource(error),
                        style = MaterialTheme.typography.titleLarge,
                        color = OnGradientVariant,
                    )
                    transcript != null -> Text(
                        text = transcript,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 44.sp),
                        color = Color.White,
                    )
                }
            }
        }

        VoiceControls(
            phase = uiState.phase,
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

private fun VoicePhase.statusLabel(): Int? = when (this) {
    VoicePhase.Idle -> null
    VoicePhase.Recording -> R.string.voice_listening
    VoicePhase.Transcribing -> R.string.voice_processing
    VoicePhase.Speaking -> R.string.voice_speaking
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
        VoiceContent(VoiceUiState(placeName = "La Tranquera", phase = VoicePhase.Recording), {}, {}, {}, {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentTranscriptPreview() {
    FildyTheme {
        VoiceContent(
            VoiceUiState(
                placeName = "La Tranquera",
                phase = VoicePhase.Speaking,
                transcript = "Llovieron 18 milímetros anoche en todo el campo",
            ),
            {}, {}, {}, {},
        )
    }
}
