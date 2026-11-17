package com.fildy.ui.voice

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fildy.R
import com.fildy.ui.components.ForestScenery
import com.fildy.ui.components.OrbBreathing
import com.fildy.ui.components.VoiceOrb
import com.fildy.ui.components.VoiceWaveform
import com.fildy.ui.theme.FildyMotion
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.OnGradientVariant

@Composable
fun VoiceScreen(
    onKeyboardClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    startRecording: Boolean = false,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Whether we may use the microphone. Checked again whenever the screen comes back (e.g. after
    // granting it in the system settings).
    var hasMicPermission by remember { mutableStateOf(context.hasRecordAudioPermission()) }
    LifecycleResumeEffect(Unit) {
        hasMicPermission = context.hasRecordAudioPermission()
        onPauseOrDispose { }
    }

    // Opened from the home pill: start recording right away if we may use the microphone. Without
    // the permission, the screen opens ready and the mic button asks for it as usual.
    LaunchedEffect(startRecording) {
        if (startRecording && context.hasRecordAudioPermission()) viewModel.startRecordingOnOpen()
    }

    // TODO: Implement the proper permission requester with a cleaner UI later.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i(TAG, "RECORD_AUDIO permission ${if (granted) "granted" else "denied"}")
        hasMicPermission = granted
        if (granted) viewModel.startRecording() else viewModel.onMicrophonePermissionDenied()
    }

    VoiceContent(
        uiState = uiState,
        hasMicPermission = hasMicPermission,
        onStartRecording = {
            if (context.hasRecordAudioPermission()) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onStopRecording = viewModel::stopRecording,
        onKeyboardClick = onKeyboardClick,
        onClose = onClose,
        modifier = modifier,
    )
}

private const val TAG = "Fildy.Voice"

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

// Vertical positions from the design (dp from the top of the safe area).
private val OrbTopIdle = 202.dp
private val OrbSizeIdle = 220.dp
private val OrbTopCompact = 30.dp
private val OrbSizeCompact = 64.dp
private val PromptTop = OrbTopIdle + OrbSizeIdle + 46.dp
private val StatusLabelTop = OrbTopCompact + OrbSizeCompact + 8.dp
private val TranscriptTop = StatusLabelTop + 130.dp

// While recording and while the answer is read out loud: the sphere is big, near the top, so the
// waveform or the answer still fit below it.
private val OrbTopBig = 30.dp
private val OrbSizeBig = OrbSizeIdle
private val OrbBottomBig = OrbTopBig + OrbSizeBig
// Recording: "Te escucho…" under the big sphere, then the waveform, then the hint.
private val RecordingTitleTop = OrbBottomBig + 16.dp
private val WaveformTopBig = RecordingTitleTop + 52.dp
private val RecordingHintTop = WaveformTopBig + 56.dp
// Later recordings have no prompt: the waveform sits right under the sphere.
private val WaveformTopNoPrompt = OrbBottomBig + 28.dp
// Speaking has no status label: the answer starts below the sphere, leaving room for its pulse.
private val AnswerTopSpeaking = OrbBottomBig + 36.dp

/** Bottom space kept free for the voice controls (buttons + hint). */
private val ControlsClearance = 170.dp

@Composable
fun VoiceContent(
    uiState: VoiceUiState,
    hasMicPermission: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onKeyboardClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = uiState.isCompact
    val speaking = uiState.phase == VoicePhase.Speaking
    val recording = uiState.phase == VoicePhase.Recording
    val recordingPrompt = recording && uiState.showRecordingPrompt
    // Someone is talking (the user or the app): the sphere is big.
    val bigAtTop = speaking || recording
    val orbSpec = tween<Dp>(FildyMotion.EmphasizedDuration, easing = FildyMotion.Emphasized)
    // Three layouts: idle (big, centered, with the prompt), recording/speaking (big, at the top,
    // waveform or answer below) and the rest of the conversation (small, at the top).
    val orbTop by animateDpAsState(
        targetValue = when {
            bigAtTop -> OrbTopBig
            compact -> OrbTopCompact
            else -> OrbTopIdle
        },
        animationSpec = orbSpec,
        label = "orbTop",
    )
    val orbSize by animateDpAsState(
        targetValue = when {
            bigAtTop -> OrbSizeBig
            compact -> OrbSizeCompact
            else -> OrbSizeIdle
        },
        animationSpec = orbSpec,
        label = "orbSize",
    )
    val answerTop by animateDpAsState(if (speaking) AnswerTopSpeaking else TranscriptTop, orbSpec, label = "answerTop")
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
                // Quick while the user talks; a clear, deeper heartbeat while the app talks back.
                breathing = when {
                    speaking -> OrbBreathing.Speaking
                    uiState.phase == VoicePhase.Recording -> OrbBreathing.Listening
                    else -> OrbBreathing.Calm
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = orbTop)
                    .size(orbSize),
            )

            // Status under the small sphere: Procesando… / Pensando…. Recording shows its own prompt
            // and speaking needs none: the big pulsing sphere already shows it.
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
                // Without the permission: explain why and that the button asks for it. With it (the
                // screen opened without starting to record): invite to talk.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PromptTitle(stringResource(if (hasMicPermission) R.string.voice_ready_title else R.string.voice_permission_title))
                    PromptHint(
                        text = stringResource(if (hasMicPermission) R.string.voice_ready_hint else R.string.voice_permission_hint),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // First recording since the screen opened: "Te escucho…" under the big sphere and the
            // hint under the waveform. Later recordings skip it.
            AnimatedVisibility(
                visible = recordingPrompt,
                enter = fadeIn(fade),
                exit = fadeOut(fade),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = RecordingTitleTop),
            ) {
                PromptTitle(stringResource(R.string.voice_prompt_title))
            }
            AnimatedVisibility(
                visible = recordingPrompt,
                enter = fadeIn(fade),
                exit = fadeOut(fade),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RecordingHintTop, start = 24.dp, end = 24.dp),
            ) {
                PromptHint(stringResource(R.string.voice_prompt_hint))
            }

            // Voice bars while the microphone is capturing.
            AnimatedVisibility(
                visible = uiState.phase == VoicePhase.Recording,
                enter = fadeIn(fade),
                exit = fadeOut(fade),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (uiState.showRecordingPrompt) WaveformTopBig else WaveformTopNoPrompt),
            ) {
                VoiceWaveform(levels = uiState.audioLevels)
            }

            // The assistant's answer with its data cards, or what went wrong. Scrolls when long,
            // stopping above the bottom controls.
            AnimatedContent(
                targetState = Triple(uiState.answer, uiState.answerCards, uiState.errorMessage),
                transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = answerTop, bottom = ControlsClearance),
                label = "voiceAnswer",
            ) { (answer, cards, error) ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    when {
                        error != null -> Text(
                            text = stringResource(error),
                            style = MaterialTheme.typography.titleLarge,
                            color = OnGradientVariant,
                        )
                        // Title L (22/30 bold), as in the design's answer screen: answers can be long.
                        answer != null -> Text(
                            text = answer,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    }
                    cards.forEach { AnswerCardView(it) }
                }
            }
        }

        VoiceControls(
            phase = uiState.phase,
            hasMicPermission = hasMicPermission,
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

@Composable
private fun PromptTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, letterSpacing = (-0.3).sp),
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun PromptHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        color = OnGradientVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private fun VoicePhase.statusLabel(): Int? = when (this) {
    VoicePhase.Idle -> null
    VoicePhase.Recording -> null
    VoicePhase.Transcribing -> R.string.voice_processing
    VoicePhase.Thinking -> R.string.voice_thinking
    VoicePhase.Speaking -> null
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentNoPermissionPreview() {
    FildyTheme {
        VoiceContent(VoiceUiState(placeName = "La Tranquera"), hasMicPermission = false, {}, {}, {}, {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentIdlePreview() {
    FildyTheme {
        VoiceContent(VoiceUiState(placeName = "La Tranquera"), hasMicPermission = true, {}, {}, {}, {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentRecordingPreview() {
    FildyTheme {
        VoiceContent(
            VoiceUiState(
                placeName = "La Tranquera",
                phase = VoicePhase.Recording,
                audioLevels = listOf(0.2f, 0.33f, 0.6f, 0.86f, 0.66f, 0.4f, 0.73f, 1f, 0.8f, 0.46f, 0.33f, 0.6f),
            ),
            hasMicPermission = true,
            onStartRecording = {}, onStopRecording = {}, onKeyboardClick = {}, onClose = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentAnswerPreview() {
    FildyTheme {
        VoiceContent(
            VoiceUiState(
                placeName = "La Tranquera",
                phase = VoicePhase.Speaking,
                answer = "Anoche llovieron 18 milímetros en todo el campo.",
                answerCards = listOf(AnswerCard.RainDay(java.time.LocalDate.of(2026, 11, 15), 18.0)),
            ),
            hasMicPermission = true,
            onStartRecording = {}, onStopRecording = {}, onKeyboardClick = {}, onClose = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun VoiceContentFollowUpRecordingPreview() {
    FildyTheme {
        VoiceContent(
            VoiceUiState(
                placeName = "La Tranquera",
                phase = VoicePhase.Recording,
                audioLevels = listOf(0.2f, 0.33f, 0.6f, 0.86f, 0.66f, 0.4f, 0.73f, 1f, 0.8f, 0.46f, 0.33f, 0.6f),
                showRecordingPrompt = false,
            ),
            hasMicPermission = true,
            onStartRecording = {}, onStopRecording = {}, onKeyboardClick = {}, onClose = {},
        )
    }
}
