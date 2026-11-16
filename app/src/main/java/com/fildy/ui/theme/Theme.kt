package com.fildy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The brand stays the same on every phone: no dynamic color (Material You) and no dark palette.
private val FildyColorScheme = lightColorScheme(
    primary = Pino,
    onPrimary = Blanco,
    primaryContainer = Bosque,
    onPrimaryContainer = Blanco,
    secondary = Menta,
    onSecondary = Tinta,
    tertiary = Brote,
    onTertiary = Bosque,
    background = Niebla,
    onBackground = Tinta,
    surface = Niebla,
    onSurface = Tinta,
    onSurfaceVariant = Musgo,
    surfaceContainerLowest = Blanco,
    outline = Musgo,
    outlineVariant = Outline,
)

@Composable
fun FildyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FildyColorScheme,
        typography = Typography,
        content = content,
    )
}
