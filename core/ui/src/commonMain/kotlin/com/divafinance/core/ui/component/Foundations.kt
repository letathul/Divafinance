package com.divafinance.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.adaptive.DivaGroupHeader
import com.divafinance.core.ui.theme.diva

/** Mono, uppercase, wide-tracked. The eyebrow above a figure. */
@Composable
fun Meta(text: String, modifier: Modifier = Modifier, color: Color = diva.muted) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/**
 * The label above a section.
 *
 * Delegates to [DivaGroupHeader] so a screen that has not been converted to grouped
 * lists yet still picks up the platform's own header treatment.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) = DivaGroupHeader(
    text = title,
    modifier = modifier,
    trailing = trailing,
    onTrailingClick = onTrailingClick,
)

/**
 * Selected state inverts fore- and background together rather than tinting, so contrast
 * never drops below the resting state.
 *
 * [onLongPress] is optional and receives the index that was held. It is a way to offer a
 * fuller choice than the visible segments without spending a row on a second control.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onLongPress: ((Int) -> Unit)? = null,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(Pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, diva.fgHair), Pill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                Modifier
                    .clip(Pill)
                    .background(
                        if (on) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
                    )
                    .then(
                        // `clickable` when there is nothing to hold for, so the plain case
                        // keeps its shorter press-to-fire timing.
                        if (onLongPress == null) {
                            Modifier.clickable { onSelect(i) }
                        } else {
                            Modifier.combinedClickable(
                                onLongClick = { onLongPress(i) },
                                onClick = { onSelect(i) },
                            )
                        }
                    )
                    .padding(horizontal = Space.pad, vertical = Space.sm),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) MaterialTheme.colorScheme.onSurface else diva.muted,
                )
            }
        }
    }
}

/**
 * Budget / threshold progress.
 *
 * Over-budget swaps to [DivaTokens.negative] rather than to a decorative gradient: being
 * over is a warning, and it should read as one.
 */
@Composable
fun BudgetTrack(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    isOver: Boolean = false,
    height: Dp = 8.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(Pill)
            .background(diva.fgHair)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(Pill)
                .background(if (isOver) diva.negative else color)
        )
    }
}

/** The trend / status pill: "$31.04 over pace", "7.6% vs last week". */
@Composable
fun StatPill(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .clip(Pill)
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** A figure, in the one style every figure in the app uses. */
@Composable
fun Numeric(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = NumericStyle,
) {
    Text(text, modifier = modifier, style = style, color = color)
}

/** Hairline divider: the card border's 12% rule on Material, a 0.5pt separator on iOS. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(diva.hairline).background(diva.separator))
}

/**
 * A spacer that takes up the height of the system status bars. Use this at the top of
 * scrollable content or screens to ensure they clear the edge-to-edge status bar while
 * allowing the background to draw behind it.
 */
@Composable
fun StatusBarSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier.statusBarsPadding())
}
