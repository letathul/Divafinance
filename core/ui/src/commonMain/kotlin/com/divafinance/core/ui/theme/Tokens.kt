package com.divafinance.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.enums.SpendingCategory

/**
 * The tokens Material 3 has no slot for, plus the handful of measurements that differ
 * between the two platform languages.
 *
 * Metrics live here rather than in [Space] because they are platform-dependent: a
 * Cupertino row is 44dp and separated by a 0.5dp rule, a Material one is 56dp and
 * separated by tone. A screen reads `diva.rowMinHeight` and gets the right answer
 * without branching.
 */
@Immutable
data class DivaTokens(
    val platform: DivaPlatform,
    /** The user's chosen accent, resolved for this scheme. */
    val accentColor: Color,
    /** The brand mark's colour. Independent of [accentColor] — see `DivaBrandLight`. */
    val brand: Color,
    /** A third text level between `onSurface` and disabled. */
    val muted: Color,
    /** The 12% rule this design separates surfaces with instead of elevation. */
    val fgHair: Color,
    /** The rule *inside* a grouped list. On Cupertino this is a real system colour. */
    val separator: Color,
    val negative: Color,
    val positive: Color,
    /** The screen canvas a grouped list sits on. */
    val canvas: Color,
    /** The inset card a run of rows lives in. */
    val card: Color,
    /** A bar's fill before its translucency is applied. */
    val barFill: Color,
    /** The resting fill of a keypad operator key. */
    val keyFill: Color,
    val hairline: Dp,
    val cardRadius: Dp,
    val rowMinHeight: Dp,
    val isDark: Boolean,
    private val categories: Map<SpendingCategory, Color>,
) {
    /**
     * The accent as a single colour.
     *
     * Reach for this only where something should stay accented on **both** platforms —
     * the story rings, the avatar ring, the raised compose button. Anything that is
     * "the tinted affordance" should go through `colorScheme.primary` instead, which is
     * the neutral foreground on Material and the accent on Cupertino.
     */
    val accent: Color get() = accentColor

    fun categoryColor(category: SpendingCategory): Color =
        categories[category] ?: categories.getValue(SpendingCategory.OTHER)
}

val LocalDivaTokens = staticCompositionLocalOf<DivaTokens> {
    error("DivaTokens not provided — wrap the tree in DivaTheme { }")
}

/** Shorthand: `diva.muted`, `diva.canvas`, alongside `MaterialTheme.colorScheme`. */
val diva: DivaTokens
    @Composable @ReadOnlyComposable get() = LocalDivaTokens.current

internal fun divaTokens(
    dark: Boolean,
    accent: AccentTheme,
    platform: DivaPlatform,
): DivaTokens {
    val cupertino = platform == DivaPlatform.CUPERTINO
    val fg = if (dark) DarkForeground else LightForeground
    return DivaTokens(
        platform = platform,
        accentColor = accent.tintFor(dark),
        brand = if (dark) DivaBrandDark else DivaBrandLight,
        muted = if (cupertino) SystemGrey else if (dark) DarkMuted else LightMuted,
        fgHair = fg.copy(alpha = if (dark) 0.12f else 0.10f),
        separator = when {
            !cupertino -> fg.copy(alpha = if (dark) 0.12f else 0.10f)
            dark -> SeparatorDark
            else -> SeparatorLight
        },
        negative = when {
            !cupertino -> if (dark) DarkNegative else LightNegative
            dark -> SystemRedDark
            else -> SystemRed
        },
        positive = when {
            !cupertino -> if (dark) DarkPositive else LightPositive
            dark -> SystemGreenDark
            else -> SystemGreen
        },
        canvas = when {
            !cupertino -> if (dark) DarkBackground else LightBackground
            dark -> GroupedDark
            else -> GroupedLight
        },
        card = when {
            !cupertino -> if (dark) DarkSurface else LightSurface
            dark -> CardDark
            else -> CardLight
        },
        barFill = when {
            !cupertino -> if (dark) DarkSurface else LightSurface
            dark -> BarDark
            else -> BarLight
        },
        keyFill = when {
            !cupertino -> if (dark) DarkSurfaceHigh else LightSurfaceHigh
            dark -> KeyGreyDark
            else -> KeyGreyLight
        },
        hairline = if (cupertino) 0.5.dp else 1.dp,
        cardRadius = if (cupertino) 14.dp else 18.dp,
        rowMinHeight = if (cupertino) 44.dp else 56.dp,
        isDark = dark,
        categories = categoryRamp(dark),
    )
}

/** Fully-rounded. Chips, pills and tracks all share it. */
val Pill = RoundedCornerShape(50)

/** The spacing scale. `pad` is the screen gutter every screen aligns to. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val pad = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
