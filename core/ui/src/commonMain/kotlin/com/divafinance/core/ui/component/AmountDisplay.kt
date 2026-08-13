package com.divafinance.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.divafinance.core.ui.theme.DivaGreen
import com.divafinance.core.ui.theme.DivaRed

@Composable
fun AmountDisplay(
    amount: Double,
    currency: String = "USD",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    showSign: Boolean = false,
) {
    val color = when {
        amount > 0 && showSign -> DivaGreen
        amount < 0 -> DivaRed
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = when {
        amount > 0 && showSign -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    val formatted = "$prefix$currency ${"%.2f".format(kotlin.math.abs(amount))}"

    Text(
        text = formatted,
        style = style,
        color = color,
        modifier = modifier,
    )
}
