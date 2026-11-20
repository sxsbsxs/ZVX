package com.zvx.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ZvxDarkColors = darkColorScheme(
    primary = ZvxAccent,
    onPrimary = ZvxBackground,
    secondary = ZvxMuted,
    onSecondary = ZvxBackground,
    background = ZvxBackground,
    onBackground = ZvxText,
    surface = ZvxSurface,
    onSurface = ZvxText,
    surfaceVariant = ZvxSurfaceRaised,
    onSurfaceVariant = ZvxMuted,
    error = ZvxDanger
)

@Composable
fun ZvxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZvxDarkColors,
        content = content
    )
}
