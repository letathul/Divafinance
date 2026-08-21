package com.divafinance.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.enums.SpendingCategory

/**
 * The tokens Material 3 has no slot for.
 *
 * `muted` is a third text level between `onSurface` and disabled; `fgHair` is the 12%
 * rule the whole design separates surfaces with instead of elevation; `sweep` is the
 * accent gradient, which is deliberately *not* a colour-scheme entry so it cannot leak
 * into an ordinary component.
 */
@Immutable
data class DivaTokens(
    val muted: Color,
    val fgHair: Color,
    val negative: Color,
    val positive: Color,
    val sweepColors: List<Color>,
    val isDark: Boolean,
    private val categories: Map<SpendingCategory, Color>,
) {
    /** Left-to-right sweep. The default direction for tracks, buttons and borders. */
    val sweep: Brush get() = Brush.horizontalGradient(sweepColors)

    /** The ring treatment — a full turn, so the seam lands back on the start colour. */
    val sweepConic: Brush get() = Brush.sweepGradient(sweepColors + sweepColors.first())

    fun categoryColor(category: SpendingCategory): Color =
        categories[category] ?: categories.getValue(SpendingCategory.OTHER)

    /** The accent as a single colour, for the rare place a gradient will not fit. */
    val accent: Color get() = sweepColors[sweepColors.size / 2]
}

val LocalDivaTokens = staticCompositionLocalOf<DivaTokens> {
    error("DivaTokens not provided — wrap the tree in DivaTheme { }")
}

/** Shorthand: `diva.muted`, `diva.sweep`, alongside `MaterialTheme.colorScheme`. */
val diva: DivaTokens
    @Composable @ReadOnlyComposable get() = LocalDivaTokens.current

internal fun divaTokens(dark: Boolean, accent: AccentTheme): DivaTokens {
    val fg = if (dark) DarkForeground else LightForeground
    return DivaTokens(
        muted = if (dark) DarkMuted else LightMuted,
        fgHair = fg.copy(alpha = if (dark) 0.12f else 0.10f),
        negative = if (dark) DarkNegative else LightNegative,
        positive = if (dark) DarkPositive else LightPositive,
        sweepColors = accent.colors,
        isDark = dark,
        categories = categoryRamp(dark),
    )
}

/** Fully-rounded. Chips, pills, tracks and the tab capsule all share it. */
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
