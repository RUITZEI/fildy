package com.fildy.ui.home.header

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fildy.R
import com.fildy.ui.theme.FildyMotion
import com.fildy.ui.theme.FildyTheme
import com.fildy.ui.theme.OnGradientVariant
import com.fildy.ui.theme.forestBackground

/** "Hola, Francisco" and "La Tranquera · 📍 Castelli", at the top of the home screen. */
@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
    viewModel: HomeHeaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeHeader(uiState, modifier)
}

@Composable
fun HomeHeader(uiState: HomeHeaderUiState, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.home_greeting, uiState.userName),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        // The farm line fades in once the farm is loaded (first launch seeds it).
        AnimatedVisibility(
            visible = uiState.farmName != null && uiState.locality != null,
            enter = fadeIn(tween(FildyMotion.StandardDuration, easing = FildyMotion.Standard)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                val lineStyle = MaterialTheme.typography.bodyLarge
                Text(text = "${uiState.farmName} ·", style = lineStyle, color = OnGradientVariant)
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = null,
                    tint = OnGradientVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(text = uiState.locality.orEmpty(), style = lineStyle, color = OnGradientVariant)
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun HomeHeaderPreview() {
    FildyTheme {
        HomeHeader(
            HomeHeaderUiState(userName = "Francisco", farmName = "La Tranquera", locality = "Castelli"),
            Modifier
                .forestBackground()
                .padding(18.dp),
        )
    }
}
