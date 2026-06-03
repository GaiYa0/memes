package com.emoji.overlay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmojiLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF625B71),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    background = Color(0xFFFFFBFE)
)

@Composable
fun EmojiOverlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmojiLightColorScheme,
        content = content
    )
}
