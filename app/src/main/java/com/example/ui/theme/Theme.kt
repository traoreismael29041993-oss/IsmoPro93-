package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighDensityColorScheme = lightColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberTertiary,
    background = CyberBlack,
    surface = CyberDarkSurface,
    onPrimary = Color.White,
    onSecondary = LavenderOnContainer,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardBg,
    outline = CyberCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force Light/Lavender Mode as per Design Theme HTML specification
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighDensityColorScheme,
        typography = Typography,
        content = content
    )
}
