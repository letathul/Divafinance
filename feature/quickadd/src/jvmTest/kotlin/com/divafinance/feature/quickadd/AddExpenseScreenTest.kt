package com.divafinance.feature.quickadd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Drives the stateless sheet body directly. The keypad is the part most worth covering:
 * a wrong amount is the worst thing this screen can do.
 */
@OptIn(ExperimentalTestApi::class)
class AddExpenseScreenTest {

    /** Mirrors what QuickAddViewModel does, so the rendered total is the real one. */
    private class Harness {
        var state = QuickAddUiState(suggestedCategories = listOf(SpendingCategory.DINING))
            private set

        fun digit(char: Char) { state = state.copy(expression = state.expression + char) }
        fun operator(symbol: Char) { state = state.copy(expression = state.expression + symbol) }
        fun backspace() { state = state.copy(expression = state.expression.dropLast(1)) }
    }

    @Test
    fun startsAtZero() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("$0.00").assertIsDisplayed()
    }

    @Test
    fun saveIsDisabledWithoutAnAmount() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("Add expense").assertIsNotEnabled()
    }

    @Test
    fun saveIsEnabledOnceAnAmountIsEntered() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "12"),
                )
            }
        }
        onNodeWithText("Add expense").assertIsEnabled()
    }

    @Test
    fun rendersTheEvaluatedTotal() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "12+8.50"),
                )
            }
        }
        onNodeWithText("$20.50").assertIsDisplayed()
        // The expression stays visible under the total so the sum is checkable.
        onNodeWithText("12+8.50").assertIsDisplayed()
    }

    @Test
    fun showsARunningTotalForAnUnfinishedExpression() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "12+"),
                )
            }
        }
        onNodeWithText("$12.00").assertIsDisplayed()
        onNodeWithText("Add expense").assertIsNotEnabled()
    }

    @Test
    fun keypadBuildsAnExpression() = runComposeUiTest {
        val harness = Harness()
        setContent {
            DivaTheme {
                AddExpenseContent(
                    state = harness.state,
                    onDigit = harness::digit,
                    onOperator = harness::operator,
                    onBackspace = harness::backspace,
                    onTypeChange = {}, onCategoryChange = {}, onToggleAllCategories = {},
                    onToggleDetails = {}, onMerchantChange = {}, onMerchantSuggestionPicked = {},
                    onNoteChange = {},
                    onCardChange = {}, onDayChange = {}, onSave = {},
                )
            }
        }

        onNodeWithContentDescription("1").performClick()
        onNodeWithContentDescription("2").performClick()
        onNodeWithContentDescription("Plus").performClick()
        onNodeWithContentDescription("8").performClick()

        assertEquals("12+8", harness.state.expression)
    }

    @Test
    fun backspaceKeyRemovesTheLastCharacter() = runComposeUiTest {
        val harness = Harness()
        setContent {
            DivaTheme {
                AddExpenseContent(
                    state = harness.state,
                    onDigit = harness::digit,
                    onOperator = harness::operator,
                    onBackspace = harness::backspace,
                    onTypeChange = {}, onCategoryChange = {}, onToggleAllCategories = {},
                    onToggleDetails = {}, onMerchantChange = {}, onMerchantSuggestionPicked = {},
                    onNoteChange = {},
                    onCardChange = {}, onDayChange = {}, onSave = {},
                )
            }
        }

        onNodeWithContentDescription("5").performClick()
        onNodeWithContentDescription("9").performClick()
        onNodeWithContentDescription("Backspace").performClick()

        assertEquals("5", harness.state.expression)
    }

    @Test
    fun showsSuggestedCategoryChips() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        suggestedCategories = listOf(SpendingCategory.GAS, SpendingCategory.DINING),
                        category = SpendingCategory.GAS,
                    ),
                    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        }
        onNodeWithText("Gas").assertIsDisplayed()
        onNodeWithText("Dining").assertIsDisplayed()
        onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun detailsAreHiddenUntilExpanded() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("Add details").assertIsDisplayed()
    }

    // --- the place line -----------------------------------------------------

    @Test
    fun asksWhereWhenNothingHasBeenCaptured() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("Where?").assertIsDisplayed()
    }

    /** The switch is gone: the place line is now the only way in. */
    @Test
    fun offersNoSeparateLocationSwitch() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("Add location").assertDoesNotExist()
    }

    @Test
    fun tappingWhereAsksForALocation() = runComposeUiTest {
        var tapped = 0
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onWhereTapped = { tapped++ })
            }
        }
        onNodeWithText("Where?").performClick()

        assertEquals(1, tapped)
    }

    /** Once there is a fix the line becomes the place, and stays tappable to re-read it. */
    @Test
    fun theCapturedPlaceReplacesTheQuestionAndStillTaps() = runComposeUiTest {
        var tapped = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        location = LocationTag(51.5, -0.12),
                        locationName = "Trafalgar Square",
                    ),
                    onWhereTapped = { tapped++ },
                )
            }
        }
        onNodeWithText("Where?").assertDoesNotExist()
        // By description, not text: the editable "Place" field carries the same name.
        onNodeWithContentDescription("Update where you are").performClick()

        assertEquals(1, tapped)
    }

    @Test
    fun offersTheCaptureChoiceOnFirstUse() = runComposeUiTest {
        var chosen: LocationCaptureMode? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(locationPrompt = LocationPrompt.CHOICE),
                    onLocationCaptureModeChosen = { chosen = it },
                )
            }
        }
        onNodeWithText("Remember where you spend?").assertIsDisplayed()
        onNodeWithText("Every time").performClick()

        assertEquals(LocationCaptureMode.ALWAYS, chosen)
    }

    /** The system dialog cannot say why, so this must appear before it, not after. */
    @Test
    fun explainsItselfBeforeTheOsPrompt() = runComposeUiTest {
        var accepted = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(locationPrompt = LocationPrompt.RATIONALE),
                    onLocationRationaleAccepted = { accepted++ },
                )
            }
        }
        onNodeWithText("Location permission").assertIsDisplayed()
        onNodeWithText("Continue").performClick()

        assertEquals(1, accepted)
    }

    @Test
    fun showsAnErrorWhenSaveIsRejected() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(error = "Enter an amount greater than zero"),
                )
            }
        }
        onNodeWithText("Enter an amount greater than zero").assertIsDisplayed()
    }
}
