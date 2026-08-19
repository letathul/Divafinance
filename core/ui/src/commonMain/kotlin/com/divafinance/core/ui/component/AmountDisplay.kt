package com.divafinance.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.util.formatCurrency
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AmountDisplay(
    amount: Double,
    currency: String = "USD",
    modifier: Modifier = Modifier,
    style: TextStyle = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
    showSign: Boolean = false,
) {
    val color = when {
        amount > 0 && showSign -> diva.positive
        amount < 0 -> diva.negative
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = when {
        amount > 0 && showSign -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    val formatted = "$prefix${formatCurrency(amount, currency)}"

    Text(
        text = formatted,
        style = style,
        color = color,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AmountDisplayPreview() {
    DivaTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AmountDisplay(amount = 1250.50, showSign = true)
            AmountDisplay(amount = -89.99)
            AmountDisplay(amount = 0.0)
        }
    }
}
