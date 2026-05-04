package com.odom.photocard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElderColorScheme = lightColorScheme(
    primary = ElderBlue,
    onPrimary = Color.White,
    primaryContainer = ElderBlueContainer,
    onPrimaryContainer = ElderBlueDark,
    secondary = ElderOrange,
    onSecondary = Color.White,
    secondaryContainer = ElderOrangeContainer,
    onSecondaryContainer = Color(0xFF4E1F00),
    tertiary = Color(0xFF1B5E20),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = TextOnLight,
    surface = Color.White,
    onSurface = TextOnLight,
    surfaceVariant = PanelSurface,
    onSurfaceVariant = TextOnLight,
    surfaceContainer = PanelSurface,
    surfaceContainerHigh = ElderBlueContainer,
    error = ElderRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = ElderRed,
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFFBDBDBD),
)

// Always light — no dark mode for elderly users
@Composable
fun PhotoCardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ElderColorScheme,
        typography = Typography,
        content = content
    )
}
