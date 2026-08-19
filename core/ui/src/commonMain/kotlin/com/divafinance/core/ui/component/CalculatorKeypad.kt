package com.divafinance.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.diva

/** A key on the pad. Operators are visually distinct from digits. */
sealed interface Key {
    data class Digit(val char: Char) : Key
    data class Operator(val symbol: Char, val label: String) : Key
    data object Backspace : Key
}

private val KEY_ROWS: List<List<Key>> = listOf(
    listOf(Key.Digit('7'), Key.Digit('8'), Key.Digit('9'), Key.Operator('/', "÷")),
    listOf(Key.Digit('4'), Key.Digit('5'), Key.Digit('6'), Key.Operator('*', "×")),
    listOf(Key.Digit('1'), Key.Digit('2'), Key.Digit('3'), Key.Operator('-', "−")),
    listOf(Key.Digit('.'), Key.Digit('0'), Key.Backspace, Key.Operator('+', "+")),
)

/**
 * Numeric pad with arithmetic, so an amount can be entered as "12+8.50" or "120/3"
 * without leaving for a calculator. Replaces the soft keyboard entirely — the field it
 * feeds is never focusable.
 */
@Composable
fun CalculatorKeypad(
    onDigit: (Char) -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KEY_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                is Key.Digit -> onDigit(key.char)
                                is Key.Operator -> onOperator(key.symbol)
                                Key.Backspace -> onBackspace()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(key: Key, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isOperator = key is Key.Operator || key is Key.Backspace
    val label = when (key) {
        is Key.Digit -> key.char.toString()
        is Key.Operator -> key.label
        Key.Backspace -> "⌫"
    }
    // The symbol shown on an operator key is typographic (× ÷ −) and would be useless to
    // a screen reader, and to a test looking for the key it should press.
    val description = when (key) {
        is Key.Digit -> if (key.char == '.') "Decimal point" else key.char.toString()
        is Key.Operator -> when (key.symbol) {
            '+' -> "Plus"
            '-' -> "Minus"
            '*' -> "Multiply"
            else -> "Divide"
        }
        Key.Backspace -> "Backspace"
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = if (isOperator) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (isOperator) diva.muted else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, diva.fgHair),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
