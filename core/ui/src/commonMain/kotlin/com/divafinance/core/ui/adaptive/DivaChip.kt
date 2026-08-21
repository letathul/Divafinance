package com.divafinance.core.ui.adaptive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino

enum class DivaChipStyle {
    /** Sits on the canvas as a card would. */
    Filled,

    /** A hairline outline and nothing else — the disclosure chips under the keypad. */
    Outlined,

    /** A muted fill. The default. */
    Tonal,
}

/**
 * The pill.
 *
 * Selected reads as tinted-on-tint rather than inverted, which is what both platform
 * languages do with a chosen chip.
 *
 * [onLongClick] is not decoration: the add-expense screen offers a fuller choice behind a
 * hold on its day and split chips, and its tests drive that gesture.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DivaChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    style: DivaChipStyle = DivaChipStyle.Tonal,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val background = when {
        selected -> accent.copy(alpha = if (isCupertino) 0.12f else 0.16f)
        style == DivaChipStyle.Outlined -> Color.Transparent
        style == DivaChipStyle.Filled -> diva.card
        else -> if (isCupertino) diva.card else MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        !enabled -> diva.muted
        selected -> accent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier
            .clip(Pill)
            .background(background)
            .then(
                if (style == DivaChipStyle.Outlined && !selected) {
                    Modifier.border(BorderStroke(diva.hairline, diva.separator), Pill)
                } else {
                    Modifier
                }
            )
            .then(
                when {
                    !enabled || onClick == null -> Modifier
                    // `clickable` when there is nothing to hold for, so the plain case
                    // keeps its shorter press-to-fire timing.
                    onLongClick == null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick)
                }
            )
            .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
        )
    }
}
