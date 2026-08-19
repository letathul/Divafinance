package com.divafinance.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CreditCardVisual(
    name: String,
    lastFour: String?,
    network: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // The card's own colour, faded into the surface rather than laid on flat — a solid
    // block of arbitrary user-picked colour is the one thing that breaks this palette.
    val onCard = if (color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.matchParentSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = onCard,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = lastFour?.let { "**** **** **** $it" } ?: "**** **** **** ****",
                    style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    color = onCard.copy(alpha = 0.9f),
                )
                Text(
                    text = network,
                    style = MaterialTheme.typography.labelSmall,
                    color = onCard.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CreditCardVisualPreview() {
    DivaTheme {
        CreditCardVisual(
            name = "Chase Sapphire",
            lastFour = "4242",
            network = "VISA",
            color = Color(0xFF7CB0DA),
        )
    }
}
