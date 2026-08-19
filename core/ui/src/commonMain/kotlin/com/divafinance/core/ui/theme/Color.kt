package com.divafinance.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.divafinance.core.model.enums.SpendingCategory

/*
 * Two ramps, hand-tuned rather than derived from one another. Inverting a dark scheme
 * gives you muddy mid-tones and destroys the category separation, so light is its own
 * set of values chosen to hit the same contrast ratios against its own background.
 */

// ── dark ──────────────────────────────────────────────────────────────────────
val DarkBackground = Color(0xFF0B090D)
val DarkSurface = Color(0xFF18151A)
val DarkSurfaceHigh = Color(0xFF252227)
val DarkForeground = Color(0xFFFCFCFC)
val DarkMuted = Color(0xFFA19DA4)
val DarkNegative = Color(0xFFD66765)
val DarkPositive = Color(0xFF7FC69B)

// ── light ─────────────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFFBFAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF1EFF3)
val LightForeground = Color(0xFF16141A)
val LightMuted = Color(0xFF6B6670)
val LightNegative = Color(0xFFC1443F)
val LightPositive = Color(0xFF3F8C5F)

/**
 * The category ramp is data encoding, not decoration: twelve hues spread evenly around
 * the wheel at one lightness and one chroma, so no category reads as louder than another
 * and adjacent slices in a chart stay separable. Used for tiles, dots and chart series —
 * never for chrome.
 *
 * Light mode needs its own, darker set. The dark ramp is tuned for a near-black canvas
 * and collapses to about 2:1 against white, so it is re-derived rather than reused.
 */
private val DarkCategoryRamp: Map<SpendingCategory, Color> = mapOf(
    SpendingCategory.DINING to Color(0xFFD9A05C),
    SpendingCategory.GAS to Color(0xFFC9B27A),
    SpendingCategory.GROCERIES to Color(0xFFA8C282),
    SpendingCategory.TRANSPORTATION to Color(0xFF7FC69B),
    SpendingCategory.UTILITIES to Color(0xFF74C3BE),
    SpendingCategory.TRAVEL to Color(0xFF7CB0DA),
    SpendingCategory.SUBSCRIPTIONS to Color(0xFF8FA3E0),
    SpendingCategory.ENTERTAINMENT to Color(0xFFB197DD),
    SpendingCategory.SHOPPING to Color(0xFFD294D2),
    SpendingCategory.HEALTHCARE to Color(0xFFE0919F),
    SpendingCategory.EDUCATION to Color(0xFFE09E85),
    SpendingCategory.OTHER to Color(0xFFA9A3AE),
)

private val LightCategoryRamp: Map<SpendingCategory, Color> = mapOf(
    SpendingCategory.DINING to Color(0xFF9A6420),
    SpendingCategory.GAS to Color(0xFF8A7534),
    SpendingCategory.GROCERIES to Color(0xFF66823D),
    SpendingCategory.TRANSPORTATION to Color(0xFF3B8659),
    SpendingCategory.UTILITIES to Color(0xFF2F837E),
    SpendingCategory.TRAVEL to Color(0xFF39719B),
    SpendingCategory.SUBSCRIPTIONS to Color(0xFF5064A3),
    SpendingCategory.ENTERTAINMENT to Color(0xFF74589F),
    SpendingCategory.SHOPPING to Color(0xFF965594),
    SpendingCategory.HEALTHCARE to Color(0xFFA25060),
    SpendingCategory.EDUCATION to Color(0xFFA25E46),
    SpendingCategory.OTHER to Color(0xFF6B6570),
)

internal fun categoryRamp(dark: Boolean): Map<SpendingCategory, Color> =
    if (dark) DarkCategoryRamp else LightCategoryRamp

/**
 * The accent gradient. One accent, at most twice a screen — which is why it lives here
 * as a standalone sweep rather than in the [androidx.compose.material3.ColorScheme]:
 * `primary` is deliberately the neutral foreground, so a solid button can never
 * accidentally come out gradient-coloured.
 */
enum class AccentTheme(val label: String, val colors: List<Color>) {
    /** Sampled from the reference design's compose button. */
    SUNSET(
        "Sunset",
        listOf(Color(0xFFA259B0), Color(0xFFCC5F83), Color(0xFFD97561), Color(0xFFDC8A5F)),
    ),
    OCEAN(
        "Ocean",
        listOf(Color(0xFF4A7DD1), Color(0xFF4FA8C4), Color(0xFF56C4A8)),
    ),
    /** For anyone who wants no hue at all; still a gradient so the shape reads the same. */
    MONO(
        "Mono",
        listOf(Color(0xFFF2F0F3), Color(0xFFA19DA4)),
    );

    companion object {
        fun fromName(name: String?): AccentTheme =
            entries.firstOrNull { it.name == name } ?: SUNSET
    }
}
