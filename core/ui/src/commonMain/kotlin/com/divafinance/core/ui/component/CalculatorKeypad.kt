package com.divafinance.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.NumericStyle
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
 * How tall a key gets once the width has been shared out.
 *
 * Keys take the full width — a pad that leaves a gutter down each side is throwing away
 * the target area it is asking to be aimed at — and stay square while that fits. Past this
 * they stop growing in height rather than pushing five rows off a phone, which is where
 * the reference design lands too: keys a little wider than they are tall.
 */
private val MaxKeyHeight = 68.dp

/**
 * Numeric pad with arithmetic, so an amount can be entered as "12+8.50" or "120/3"
 * without leaving for a calculator. Replaces the soft keyboard entirely — the field it
 * feeds is never focusable.
 *
 * **The grid is square and identical on both platforms.** Keys are sized from the width
 * available, capped at [MaxKeySide], rather than being stretched to fill it: a pad whose
 * keys are wider than they are tall is aimed at by a thumb that has to be accurate in one
 * axis and not the other. Cupertino draws Calculator's circles, Material rounded rects.
 *
 * The compact pad has deliberately **no `=` key**: the expression is evaluated live as it
 * is typed, so one would be a no-op. [extended] adds one anyway, along with `( )` and a
 * clear, because the amount sheet shows the expression and its result side by side and
 * `=` there means "fold that result back into what I am typing".
 *
 * **Every key answers.** A press ticks haptically and dips the key; `=` on an unfinished
 * expression is the one press that cannot do what it says, and reports that rather than
 * doing nothing visible — hence [onEquals] returning whether it folded. Holding ⌫ clears,
 * so resetting is not a reach for `C` in the far corner.
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
    extended: Boolean = false,
    onGroup: (Char) -> Unit = {},
    onClear: () -> Unit = {},
    onEquals: () -> Boolean = { false },
) {
    val haptics = LocalHapticFeedback.current
    val gap = if (isCupertino) 10.dp else 8.dp
    val rows = if (extended) EXTENDED_KEY_ROWS else KEY_ROWS

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Every key is the same size and the row spans the pad: the four columns divide
        // the width, and height only follows the width until it would make the grid too
        // tall to sit under a display and a button.
        val keyHeight = minOf((maxWidth - gap * 3) / 4, MaxKeyHeight)
        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    row.forEach { key ->
                        KeyButton(
                            key = key,
                            modifier = Modifier.weight(1f).height(keyHeight),
                            onClick = {
                                // The tick is the whole of the pad's feedback on a platform
                                // with no key travel, so it fires before the state changes.
                                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                when (key) {
                                    is Key.Digit -> onDigit(key.char)
                                    is Key.Operator -> onOperator(key.symbol)
                                    is Key.Group -> onGroup(key.char)
                                    Key.Backspace -> onBackspace()
                                    Key.Clear -> onClear()
                                    Key.Equals -> haptics.performHapticFeedback(
                                        // `=` is the one key that can be asked to do
                                        // something it cannot: an unfinished expression has
                                        // no result to fold in, and silence there reads as
                                        // a dead key rather than as "not yet".
                                        if (onEquals()) {
                                            HapticFeedbackType.Confirm
                                        } else {
                                            HapticFeedbackType.Reject
                                        },
                                    )
                                }
                            },
                            // Holding ⌫ empties the field. `C` stays for anyone who reaches
                            // for it, but the common correction is now under the finger
                            // already doing the correcting.
                            onLongClick = if (key == Key.Backspace) {
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClear()
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
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
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    // A key with no travel has to move to read as having been hit. Springs back rather
    // than tweening, so a fast run of digits does not queue up behind its own animation.
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(), label = "keyScale")

    Surface(
        modifier = modifier.scale(scale),
        // CircleShape is a 50% corner off the *smaller* side, so a key that is wider than
        // it is tall comes out as a stadium rather than an ellipse — still the Cupertino
        // read, and still the shape a circle becomes when the pad has room to be square.
        shape = if (cupertino) CircleShape else MaterialTheme.shapes.medium,
        color = when {
            isOperator -> diva.accent.copy(alpha = if (pressed) 0.22f else 0.12f)
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
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = interactions,
                    // HIG has no ripple; the dip above is what the Cupertino branch gets
                    // instead. Same rule as the tab bar.
                    indication = if (cupertino) null else ripple(),
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = if (onLongClick != null) "Clear" else null,
                )
                // On the clickable node rather than on the Surface, so the node a test or
                // a screen reader finds by description is the same one that carries the
                // press. Inside the Surface, so the ripple stays clipped to the key.
                .semantics { contentDescription = description },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                // Digits are figures and get tabular ones, so "1" and "8" occupy the same
                // width and the columns of the pad stay columns.
                style = if (key is Key.Digit && key.char != '.') {
                    NumericStyle.copy(fontSize = 22.sp)
                } else {
                    MaterialTheme.typography.titleLarge
                },
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
                onEquals = { true },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
