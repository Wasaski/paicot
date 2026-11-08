package com.paicot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaicotColorScheme = darkColorScheme(
    primary = Color(0xFFE53935),
    background = Color(0xFF050505),
    surface = Color(0xFF1A1A1C),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceVariant = Color(0xFF1A1A1C),
    outline = Color(0xFF8E8E93)
)

@Composable
fun PaicotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaicotColorScheme,
        content = content
    )
}
