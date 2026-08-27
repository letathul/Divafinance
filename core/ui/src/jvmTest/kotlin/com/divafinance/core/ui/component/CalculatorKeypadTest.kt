package com.divafinance.core.ui.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.ui.theme.DivaPlatform
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every case runs **twice**, once per [DivaPlatform].
 *
 * The pad branches on `isCupertino` for its shape, its gaps and its ripple, and this host
 * is the `jvm` target, which resolves to `MATERIAL` — so a case that did not pass the
 * other value would be covering half of it. The quickadd tests only ever exercise the
 * Material branch, which is why the keypad's own contract is asserted here.
 */
@OptIn(ExperimentalTestApi::class)
class CalculatorKeypadTest {

    private val platforms = DivaPlatform.entries

    /**
     * The spelled-out descriptions are the pad's accessibility contract and what every
     * test that drives it selects on. The visible labels are typographic (÷ × − ⌫) and
     * are useless to both.
     */
    @Test
    fun everyExtendedKeyIsReachableByItsSpelledOutName() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(platform = platform) {
                    CalculatorKeypad(
                        onDigit = {},
                        onOperator = {},
                        onBackspace = {},
                        extended = true,
                    )
                }
            }
            val names = listOf(
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "Decimal point", "Plus", "Minus", "Multiply", "Divide",
                "Open bracket", "Close bracket", "Clear", "Backspace", "Equals",
            )
            names.forEach { onNodeWithContentDescription(it).assertIsDisplayed() }
        }
    }

    /** The compact pad evaluates live, so an `=` on it would be a key that does nothing. */
    @Test
    fun theCompactPadHasNoEqualsOrGrouping() = platforms.forEach { platform ->
        runComposeUiTest {
            setContent {
                DivaTheme(platform = platform) {
                    CalculatorKeypad(onDigit = {}, onOperator = {}, onBackspace = {})
                }
            }
            onNodeWithContentDescription("Equals").assertDoesNotExist()
            onNodeWithContentDescription("Open bracket").assertDoesNotExist()
            onNodeWithContentDescription("Plus").assertIsDisplayed()
        }
    }

    @Test
    fun keysReportWhatWasPressed() = platforms.forEach { platform ->
        runComposeUiTest {
            val pressed = mutableListOf<String>()
            setContent {
                DivaTheme(platform = platform) {
                    CalculatorKeypad(
                        onDigit = { pressed += "digit:$it" },
                        onOperator = { pressed += "op:$it" },
                        onBackspace = { pressed += "backspace" },
                        extended = true,
                        onGroup = { pressed += "group:$it" },
                        onClear = { pressed += "clear" },
                        onEquals = { pressed += "equals"; true },
                    )
                }
            }
            onNodeWithContentDescription("7").performClick()
            onNodeWithContentDescription("Multiply").performClick()
            onNodeWithContentDescription("Open bracket").performClick()
            onNodeWithContentDescription("Backspace").performClick()
            onNodeWithContentDescription("Clear").performClick()
            onNodeWithContentDescription("Equals").performClick()

            assertEquals(
                listOf("digit:7", "op:*", "group:(", "backspace", "clear", "equals"),
                pressed,
                "on $platform",
            )
        }
    }

    /**
     * The correction that used to mean reaching for `C` in the opposite corner is now
     * under the finger already doing the correcting.
     */
    @Test
    fun holdingBackspaceClears() = platforms.forEach { platform ->
        runComposeUiTest {
            var cleared = 0
            var deleted = 0
            setContent {
                DivaTheme(platform = platform) {
                    CalculatorKeypad(
                        onDigit = {},
                        onOperator = {},
                        onBackspace = { deleted++ },
                        extended = true,
                        onClear = { cleared++ },
                    )
                }
            }
            onNodeWithContentDescription("Backspace").performTouchInput { longClick() }

            assertEquals(1, cleared, "on $platform")
            assertEquals(0, deleted, "on $platform")
        }
    }

    /** `=` is the one key that can be asked for something it cannot do, and has to say so. */
    @Test
    fun equalsReportsWhetherItFolded() = platforms.forEach { platform ->
        runComposeUiTest {
            var asked = false
            setContent {
                DivaTheme(platform = platform) {
                    CalculatorKeypad(
                        onDigit = {},
                        onOperator = {},
                        onBackspace = {},
                        extended = true,
                        onEquals = { asked = true; false },
                    )
                }
            }
            onNodeWithContentDescription("Equals").performClick()

            assertTrue(asked, "on $platform")
        }
    }
}
