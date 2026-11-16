package com.fildy.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fildy.ui.components.ForestScenery
import com.fildy.ui.components.SharedElementKeys
import com.fildy.ui.components.VoiceButton
import com.fildy.ui.components.VoiceButtonState
import com.fildy.ui.components.sharedContainer
import com.fildy.ui.home.farm.FarmCard
import com.fildy.ui.home.rain.RainCard
import com.fildy.ui.home.weather.WeatherSection

/** Space kept free at the bottom of the scroll so the last card can scroll above the voice pill. */
private val VoicePillClearance = 120.dp

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
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 18.dp, top = 30.dp, end = 18.dp, bottom = VoicePillClearance),
        ) {
            WeatherSection()
            Spacer(Modifier.height(16.dp))
            // Cards cascade in as their data loads, 60 ms apart (design: 40–80 ms).
            FarmCard(enterDelayMillis = 60)
            Spacer(Modifier.height(10.dp))
            RainCard(enterDelayMillis = 120)
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
