package com.divafinance.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

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
 * Two schemes, and the difference between them is the point.
 *
 * On Material, `primary` stays the neutral foreground — a solid button is
 * foreground-on-background and the accent only appears where something asks for
 * `diva.accent` by name. That is what has kept the accent to twice a screen.
 *
 * On Cupertino, `primary` *is* the accent, because HIG tints every button, link and
 * control with it. Making that the only difference means a screen never branches to get
 * the right tinting: route a tinted affordance through `colorScheme.primary` and it comes
 * out neutral on Android and blue on iOS by itself.
 *
 * Both fill every slot. Leaving the surfaceContainer, outline and inverse slots at their
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

private fun cupertinoDark(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.22f),
    onPrimaryContainer = accent,
    inversePrimary = accent,

    secondary = SystemGrey,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color.White,

    tertiary = SystemGrey,
    onTertiary = Color.Black,
    tertiaryContainer = CardDark,
    onTertiaryContainer = Color.White,

    background = GroupedDark,
    onBackground = Color.White,
    surface = CardDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = SystemGrey,
    surfaceTint = CardDark,
    inverseSurface = Color.White,
    inverseOnSurface = GroupedDark,

    surfaceDim = GroupedDark,
    surfaceBright = Color(0xFF2C2C2E),
    surfaceContainerLowest = GroupedDark,
    surfaceContainerLow = CardDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),

    error = SystemRedDark,
    onError = Color.White,
    errorContainer = Color(0xFF2C2C2E),
    onErrorContainer = SystemRedDark,

    outline = SystemGrey,
    outlineVariant = SeparatorDark,
    scrim = Color.Black.copy(alpha = 0.4f),
)

private fun cupertinoLight(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.12f),
    onPrimaryContainer = accent,
    inversePrimary = accent,

    secondary = SystemGrey,
    onSecondary = Color.White,
    secondaryContainer = GroupedLight,
    onSecondaryContainer = Color.Black,

    tertiary = SystemGrey,
    onTertiary = Color.White,
    tertiaryContainer = CardLight,
    onTertiaryContainer = Color.Black,

    background = GroupedLight,
    onBackground = Color.Black,
    surface = CardLight,
    onSurface = Color.Black,
    surfaceVariant = GroupedLight,
    onSurfaceVariant = SystemGrey,
    surfaceTint = CardLight,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,

    surfaceDim = GroupedLight,
    surfaceBright = CardLight,
    surfaceContainerLowest = CardLight,
    surfaceContainerLow = CardLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = GroupedLight,
    surfaceContainerHighest = Color(0xFFE5E5EA),

    error = SystemRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = SystemRed,

    outline = SystemGrey,
    outlineVariant = SeparatorLight,
    scrim = Color.Black.copy(alpha = 0.4f),
)

private fun schemeFor(platform: DivaPlatform, dark: Boolean, accent: Color): ColorScheme =
    when (platform) {
        DivaPlatform.MATERIAL -> if (dark) DarkColors else LightColors
        DivaPlatform.CUPERTINO -> if (dark) cupertinoDark(accent) else cupertinoLight(accent)
    }

/**
 * [platform] defaults to the build target's own language but is a parameter, not a
 * constant, so a preview or a test can render the other branch. Every Compose UI test in
 * this repo runs on the `jvm` host, which is [DivaPlatform.MATERIAL]; passing
 * `platform = CUPERTINO` is the only way the iOS rendering gets covered.
 */
@Composable
fun DivaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentTheme = AccentTheme.EMERALD,
    platform: DivaPlatform = LocalDivaPlatform.current,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = divaTokens(dark, accent, platform)
    CompositionLocalProvider(
        LocalDivaPlatform provides platform,
        LocalDivaTokens provides tokens,
    ) {
        MaterialTheme(
            colorScheme = schemeFor(platform, dark, tokens.accent),
            typography = divaTypography(platform),
            shapes = divaShapes(platform),
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
