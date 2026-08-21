package com.divafinance.core.ui.adaptive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino

/**
 * The label above a group.
 *
 * Cupertino draws the HIG header — uppercase, small, heavily muted, outdented into the
 * gutter. Material draws an ordinary section title.
 */
@Composable
fun DivaGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val cupertino = isCupertino
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                start = if (cupertino) Space.lg + Space.xs else Space.pad,
                end = Space.pad,
                top = Space.lg,
                bottom = if (cupertino) 6.dp else Space.md,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (cupertino) {
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = diva.muted,
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (onTrailingClick != null) {
                    Modifier.clickable(onClick = onTrailingClick)
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * The inset card a run of rows lives in.
 *
 * Cupertino: a card fill on the grouped canvas, r:14, inset from the gutter, no border —
 * the card's own contrast against the canvas does the separating. Material: the app's
 * existing tonal surface with its 12% hairline border.
 *
 * Separators between rows are the caller's, via [DivaRowDivider] — the same idiom the
 * screens already use, so converting a hand-built run of rows is a find/replace.
 */
@Composable
fun DivaGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (header != null) DivaGroupHeader(header)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.pad)
                .clip(MaterialTheme.shapes.medium)
                .background(diva.card)
                .then(
                    if (isCupertino) Modifier
                    else Modifier.border(
                        BorderStroke(diva.hairline, diva.fgHair),
                        MaterialTheme.shapes.medium,
                    )
                ),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                Modifier.padding(
                    start = if (isCupertino) Space.lg + Space.xs else Space.pad,
                    end = Space.pad,
                    top = 6.dp,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
        }
    }
}

/**
 * One row of a grouped list. Covers the nav row, the value row, the check row and the
 * switch row — [trailing] takes whatever control the row carries.
 */
@Composable
fun DivaListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    value: String? = null,
    valueColor: Color = diva.muted,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    showChevron: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val titleColor = when {
        isDestructive -> diva.negative
        !enabled -> diva.muted
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = diva.rowMinHeight)
            .padding(horizontal = Space.pad, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = diva.muted)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
        }
        trailing?.invoke(this)
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = diva.muted.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A full-width tinted action inside a group — "Export backup", "Sign out".
 *
 * Left-aligned and tinted rather than a button, because that is what a grouped list does
 * with an action on both platforms.
 */
@Composable
fun DivaListAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
) {
    Box(
        modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = diva.rowMinHeight)
            .padding(horizontal = Space.pad, vertical = Space.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                !enabled -> diva.muted
                isDestructive -> diva.negative
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
}

/**
 * The rule between two rows. Never above the first or below the last — a grouped list's
 * outer edge is the card, not a line.
 */
@Composable
fun DivaRowDivider(startInset: Dp = Space.pad, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(diva.hairline)
            .background(diva.separator)
    )
}

/** The trailing check circle a completed row carries. */
@Composable
fun DivaCheckCircle(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) diva.positive else Color.Transparent)
            .then(
                if (checked) Modifier
                else Modifier.border(diva.hairline * 2, diva.muted.copy(alpha = 0.5f), CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.66f),
            )
        }
    }
}
