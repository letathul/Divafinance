package com.divafinance.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** What the user picked in Settings. [SYSTEM] defers to the platform. */
enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/*
 * `primary` is the neutral foreground, not the accent — so a solid button is
 * foreground-on-background and the accent gradient can only appear where something asks
 * for `diva.sweep` by name. That is what keeps the accent to twice a screen instead of
 * everywhere Material decides to tint something.
 *
 * Every slot is filled. Leaving the surfaceContainer, outline and inverse slots at their
 * M3 defaults is what makes an otherwise-themed app still look stock in its sheets,
 * menus and snackbars.
 */
private val DarkColors = darkColorScheme(
    primary = DarkForeground,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceHigh,
    onPrimaryContainer = DarkForeground,
    inversePrimary = DarkBackground,

    secondary = DarkSurfaceHigh,
    onSecondary = DarkForeground,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkForeground,

    tertiary = DarkMuted,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkSurface,
    onTertiaryContainer = DarkForeground,

    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkSurface,
    onSurface = DarkForeground,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkMuted,
    surfaceTint = DarkSurface,
    inverseSurface = DarkForeground,
    inverseOnSurface = DarkBackground,

    surfaceDim = DarkBackground,
    surfaceBright = DarkSurfaceHigh,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHigh,

    error = DarkNegative,
    onError = DarkBackground,
    errorContainer = DarkSurfaceHigh,
    onErrorContainer = DarkNegative,

    outline = DarkForeground.copy(alpha = 0.22f),
    outlineVariant = DarkForeground.copy(alpha = 0.12f),
    scrim = DarkBackground.copy(alpha = 0.72f),
)

private val LightColors = lightColorScheme(
    primary = LightForeground,
    onPrimary = LightBackground,
    primaryContainer = LightSurfaceHigh,
    onPrimaryContainer = LightForeground,
    inversePrimary = LightBackground,

    secondary = LightSurfaceHigh,
    onSecondary = LightForeground,
    secondaryContainer = LightSurfaceHigh,
    onSecondaryContainer = LightForeground,

    tertiary = LightMuted,
    onTertiary = LightBackground,
    tertiaryContainer = LightSurface,
    onTertiaryContainer = LightForeground,

    background = LightBackground,
    onBackground = LightForeground,
    surface = LightSurface,
    onSurface = LightForeground,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightMuted,
    surfaceTint = LightSurface,
    inverseSurface = LightForeground,
    inverseOnSurface = LightBackground,

    surfaceDim = LightSurfaceHigh,
    surfaceBright = LightSurface,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHigh,

    error = LightNegative,
    onError = LightSurface,
    errorContainer = LightSurfaceHigh,
    onErrorContainer = LightNegative,

    outline = LightForeground.copy(alpha = 0.20f),
    outlineVariant = LightForeground.copy(alpha = 0.10f),
    scrim = LightForeground.copy(alpha = 0.40f),
)

@Composable
fun DivaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentTheme = AccentTheme.SUNSET,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalDivaTokens provides divaTokens(dark, accent)) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = DivaTypography,
            shapes = DivaShapes,
            content = content,
        )
    }
}

/**
 * Boolean overload, kept for the previews and tests that predate [ThemeMode].
 */
@Composable
fun DivaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) = DivaTheme(
    mode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
    content = content,
)
