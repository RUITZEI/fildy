package com.fildy.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fildy.ui.components.ForestScenery
import com.fildy.ui.components.SharedElementKeys
import com.fildy.ui.components.VoiceButton
import com.fildy.ui.components.VoiceButtonState
import com.fildy.ui.components.sharedContainer
import com.fildy.ui.home.weather.WeatherSection

@Composable
fun HomeScreen(
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        ForestScenery(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 18.dp, top = 30.dp, end = 18.dp),
        ) {
            WeatherSection()
        }
        VoiceButton(
            state = VoiceButtonState.Pill,
            onClick = onVoiceClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
                .sharedContainer(SharedElementKeys.VoiceButton),
        )
    }
}
