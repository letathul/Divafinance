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

/*
 * The Cupertino set. HIG semantic colours, plus the three greys the grouped-inset list
 * is built out of: a canvas, a card that sits on it, and a 0.5pt rule between rows.
 * These are exact system values — do not "improve" them, they are what makes an iOS
 * screen read as an iOS screen.
 */
val SystemBlue = Color(0xFF007AFF)
val SystemBlueDark = Color(0xFF0A84FF)
val SystemGreen = Color(0xFF34C759)
val SystemGreenDark = Color(0xFF30D158)
val SystemRed = Color(0xFFFF3B30)
val SystemRedDark = Color(0xFFFF453A)
val SystemIndigo = Color(0xFF5856D6)
val SystemIndigoDark = Color(0xFF5E5CE6)
val SystemGrey = Color(0xFF8E8E93)

val SeparatorLight = Color(0xFFE5E5EA)
val SeparatorDark = Color(0xFF38383A)
val GroupedLight = Color(0xFFF2F2F7)
val GroupedDark = Color(0xFF000000)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1C1C1E)
val KeyGreyLight = Color(0xFFD4D4D8)
val KeyGreyDark = Color(0xFF3A3A3C)

/** The bar fill, before its alpha. There is no backdrop blur on any Compose target. */
val BarLight = Color(0xFFF8F8FA)
val BarDark = Color(0xFF1D1D1F)

/**
 * The placeholder brand.
 *
 * Deliberately not systemBlue, not the selected accent, and not a member of the category
 * ramp — so dropping in the real brand colour is an edit to these two lines and nothing
 * else. Reached as `diva.brand`.
 */
val DivaBrandLight = Color(0xFF6E4BD8)
val DivaBrandDark = Color(0xFF9B7BF0)

/**
 * The pickable accent — one colour per scheme, which is what both platform languages
 * actually use.
 *
 * The stored preference is the entry *name*, so adding or renaming one is a data-visible
 * change: [fromName] falls back to [BLUE] for anything it does not recognise.
 */
enum class AccentTheme(
    val label: String,
    val tint: Color,
    val tintDark: Color,
) {
    /** The default: the platform's own accent on iOS, a calm blue on Android. */
    BLUE("Blue", SystemBlue, SystemBlueDark),
    BRAND("Diva", DivaBrandLight, DivaBrandDark),
    SUNSET("Sunset", Color(0xFFCC5F83), Color(0xFFD97561)),
    OCEAN("Ocean", Color(0xFF3E8FA8), Color(0xFF56C4A8)),
    INDIGO("Indigo", SystemIndigo, SystemIndigoDark),
    /** For anyone who wants no hue at all. */
    MONO("Mono", Color(0xFF6B6670), Color(0xFFA19DA4));

    /** The accent for a given scheme. */
    fun tintFor(dark: Boolean): Color = if (dark) tintDark else tint

    companion object {
        fun fromName(name: String?): AccentTheme =
            entries.firstOrNull { it.name == name } ?: BLUE
    }
}
