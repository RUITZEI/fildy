package com.fildy.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.fildy.ui.components.LocalSharedTransitionScope
import com.fildy.ui.home.HomeScreen
import com.fildy.ui.theme.FildyMotion
import com.fildy.ui.theme.forestBackground
import com.fildy.ui.voice.VoiceScreen

/** All screens share the forest background, so they crossfade while shared elements morph. */
private fun crossfade(): ContentTransform {
    val spec = tween<Float>(FildyMotion.EmphasizedDuration, easing = FildyMotion.Emphasized)
    return fadeIn(spec) togetherWith fadeOut(spec)
}

@Composable
fun FildyNavDisplay() {
    val backStack = rememberNavBackStack(Home)

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .fillMaxSize()
                    .forestBackground(),
                onBack = { backStack.removeLastOrNull() },
                sharedTransitionScope = this,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = { crossfade() },
                popTransitionSpec = { crossfade() },
                predictivePopTransitionSpec = { crossfade() },
                entryProvider = entryProvider {
                    entry<Home> {
                        HomeScreen(onVoiceClick = { backStack.add(Voice(startRecording = true)) })
                    }
                    entry<Voice> { key ->
                        VoiceScreen(
                            startRecording = key.startRecording,
                            onKeyboardClick = { /* TODO: Open keyboard input. */ },
                            onClose = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}
