package com.edt.ut3.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = LightText,
    background = LightBackground,
    onBackground = LightText,
    surface = LightBackground,
    onSurface = LightText,
    surfaceVariant = LightForeground,
    onSurfaceVariant = LightText,
    outlineVariant = LightTextSecondary,
    tertiary = Accent
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkBackground,
    onSurface = DarkText,
    surfaceVariant = DarkForeground,
    onSurfaceVariant = DarkText,
    outlineVariant = DarkTextSecondary,
    tertiary = Accent
)

@Composable
fun UT3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
