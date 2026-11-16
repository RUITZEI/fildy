package com.fildy.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fildy.ui.home.weather.WeatherSection
import com.fildy.ui.theme.forestBackground

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .forestBackground()
            .statusBarsPadding()
            .padding(start = 18.dp, top = 30.dp, end = 18.dp),
    ) {
        WeatherSection()
    }
}
