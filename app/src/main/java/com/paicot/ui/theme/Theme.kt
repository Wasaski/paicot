package com.paicot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PaicotColorScheme = darkColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1518),
    onPrimaryContainer = Color(0xFFFF8A80),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = Color(0xFF050505),
    onBackground = Color.White,
    surface = Color(0xFF1F1F22),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1F1F22),
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceContainer = Color(0xFF1F1F22),
    surfaceContainerHigh = Color(0xFF2A2A2E),
    surfaceContainerHighest = Color(0xFF353539),
    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFF3A1518),
    onErrorContainer = Color(0xFFFF8A80),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF3A3A3E)
)

private val PaicotTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

private val PaicotShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PaicotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaicotColorScheme,
        typography = PaicotTypography,
        shapes = PaicotShapes,
        content = content
    )
}
