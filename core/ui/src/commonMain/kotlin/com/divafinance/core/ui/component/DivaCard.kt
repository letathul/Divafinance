package com.divafinance.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The default raised surface: card fill, platform corner, no elevation.
 *
 * Separation comes from tone rather than a shadow — on a near-black canvas an elevation
 * shadow is invisible, and on white it reads as a box. Material adds a 12% hairline
 * border on top of that; Cupertino does not, because a white card on the grouped canvas
 * already has all the edge it needs.
 */
@Composable
fun DivaCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(diva.card)
        .then(
            if (isCupertino) Modifier
            else Modifier.border(BorderStroke(diva.hairline, diva.fgHair), shape)
        )
    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = content,
    )
}

@Preview
@Composable
private fun DivaCardPreview() {
    DivaTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DivaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Card Title", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Card content goes here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
