package com.divafinance.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A key on the pad. Operators are visually distinct from digits. */
sealed interface Key {
    data class Digit(val char: Char) : Key
    data class Operator(val symbol: Char, val label: String) : Key
    data object Backspace : Key

    /** Grouping, clear and evaluate — only present on the [extended] pad. */
    data class Group(val char: Char) : Key
    data object Clear : Key
    data object Equals : Key
}

private val KEY_ROWS: List<List<Key>> = listOf(
    listOf(Key.Digit('7'), Key.Digit('8'), Key.Digit('9'), Key.Operator('/', "÷")),
    listOf(Key.Digit('4'), Key.Digit('5'), Key.Digit('6'), Key.Operator('*', "×")),
    listOf(Key.Digit('1'), Key.Digit('2'), Key.Digit('3'), Key.Operator('-', "−")),
    listOf(Key.Digit('.'), Key.Digit('0'), Key.Backspace, Key.Operator('+', "+")),
)

/**
 * The five-row pad the amount sheet uses: grouping and a clear across the top, `=` where
 * the compact pad has `+`. Only worth the extra row on a surface that is *only* the
 * calculator — inline, the four-row pad keeps the rest of the form above the fold.
 */
private val EXTENDED_KEY_ROWS: List<List<Key>> = listOf(
    listOf(Key.Group('('), Key.Group(')'), Key.Clear, Key.Operator('/', "÷")),
    listOf(Key.Digit('7'), Key.Digit('8'), Key.Digit('9'), Key.Operator('*', "×")),
    listOf(Key.Digit('4'), Key.Digit('5'), Key.Digit('6'), Key.Operator('-', "−")),
    listOf(Key.Digit('1'), Key.Digit('2'), Key.Digit('3'), Key.Operator('+', "+")),
    listOf(Key.Digit('.'), Key.Digit('0'), Key.Backspace, Key.Equals),
)

/**
 * Numeric pad with arithmetic, so an amount can be entered as "12+8.50" or "120/3"
 * without leaving for a calculator. Replaces the soft keyboard entirely — the field it
 * feeds is never focusable.
 *
 * Cupertino draws Calculator's circular keys, operators on grey; Material keeps the
 * rounded-rectangle grid.
 *
 * The compact pad has deliberately **no `=` key**: the expression is evaluated live as it
 * is typed, so one would be a no-op. [extended] adds one anyway, along with `( )` and a
 * clear, because the amount sheet shows the expression and its result side by side and
 * `=` there means "fold that result back into what I am typing".
 *
 * The accessibility contract is load-bearing and must not change: the visible label on an
 * operator is typographic (÷ × − ⌫) while its `contentDescription` is spelled out
 * ("Divide", "Backspace"), and the add-expense tests select on those descriptions.
 */
@Composable
fun CalculatorKeypad(
    onDigit: (Char) -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    keySize: Dp = 60.dp,
    extended: Boolean = false,
    onGroup: (Char) -> Unit = {},
    onClear: () -> Unit = {},
    onEquals: () -> Unit = {},
) {
    val cupertino = isCupertino
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (cupertino) 10.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        (if (extended) EXTENDED_KEY_ROWS else KEY_ROWS).forEach { row ->
            Row(
                modifier = if (cupertino) Modifier else Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (cupertino) 10.dp else 8.dp),
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = if (cupertino) Modifier.size(keySize) else Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                is Key.Digit -> onDigit(key.char)
                                is Key.Operator -> onOperator(key.symbol)
                                is Key.Group -> onGroup(key.char)
                                Key.Backspace -> onBackspace()
                                Key.Clear -> onClear()
                                Key.Equals -> onEquals()
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
    // Operators and `=` are the tinted keys; grouping and clear are the grey ones.
    val isOperator = key is Key.Operator || key is Key.Equals
    val isFunction = key is Key.Backspace || key is Key.Group || key is Key.Clear
    val label = when (key) {
        is Key.Digit -> key.char.toString()
        is Key.Operator -> key.label
        is Key.Group -> key.char.toString()
        Key.Backspace -> "⌫"
        Key.Clear -> "C"
        Key.Equals -> "="
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
        is Key.Group -> if (key.char == '(') "Open bracket" else "Close bracket"
        Key.Backspace -> "Backspace"
        Key.Clear -> "Clear"
        Key.Equals -> "Equals"
    }

    val cupertino = isCupertino
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (cupertino) Modifier else Modifier.height(56.dp))
            .semantics { contentDescription = description },
        shape = if (cupertino) CircleShape else MaterialTheme.shapes.medium,
        color = when {
            isOperator -> diva.accent.copy(alpha = 0.12f)
            isFunction -> diva.keyFill
            else -> diva.card
        },
        contentColor = when {
            isOperator -> diva.accent
            // On iOS a function key is tinted too, rather than muted.
            isFunction && cupertino -> MaterialTheme.colorScheme.primary
            isFunction -> diva.muted
            else -> MaterialTheme.colorScheme.onSurface
        },
        border = if (cupertino) null else BorderStroke(diva.hairline, diva.fgHair),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

@Preview
@Composable
private fun CalculatorKeypadPreview() {
    DivaTheme {
        Surface {
            CalculatorKeypad(
                onDigit = {},
                onOperator = {},
                onBackspace = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun CalculatorKeypadExtendedPreview() {
    DivaTheme {
        Surface {
            CalculatorKeypad(
                onDigit = {},
                onOperator = {},
                onBackspace = {},
                extended = true,
                onGroup = {},
                onClear = {},
                onEquals = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
