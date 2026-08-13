package com.divafinance.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DivaGold,
    onPrimary = DivaDark,
    primaryContainer = DivaGoldLight,
    onPrimaryContainer = DivaDark,
    secondary = DivaBlue,
    onSecondary = DivaWhite,
    background = DivaWhite,
    onBackground = DivaDark,
    surface = DivaWhite,
    onSurface = DivaDark,
    surfaceVariant = DivaLightGray,
    onSurfaceVariant = DivaGray,
    error = DivaRed,
    onError = DivaWhite,
)

private val DarkColors = darkColorScheme(
    primary = DivaGold,
    onPrimary = DivaDark,
    primaryContainer = DivaDarkSurface,
    onPrimaryContainer = DivaGoldLight,
    secondary = DivaBlue,
    onSecondary = DivaDark,
    background = DivaDark,
    onBackground = DivaWhite,
    surface = DivaDarkSurface,
    onSurface = DivaWhite,
    surfaceVariant = DivaDarkSurface,
    onSurfaceVariant = DivaGray,
    error = DivaRed,
    onError = DivaDark,
)

@Composable
fun DivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DivaTypography,
        shapes = DivaShapes,
        content = content
    )
}
