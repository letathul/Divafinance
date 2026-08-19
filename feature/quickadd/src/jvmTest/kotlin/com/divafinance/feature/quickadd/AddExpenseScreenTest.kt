package com.divafinance.feature.quickadd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
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

    /** Once there is a fix the line becomes the place, and opens the place editor. */
    @Test
    fun theCapturedPlaceReplacesTheQuestionAndOpensTheEditor() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        location = LocationTag(51.5, -0.12),
                        locationName = "Trafalgar Square",
                    ),
                )
            }
        }
        onNodeWithText("Where?").assertDoesNotExist()
        // By description, not text: the editable "Place" field carries the same name.
        onNodeWithContentDescription("Update where you are").performClick()

        onNodeWithText("Where are you?").assertIsDisplayed()
    }

    /** Re-reading the position is a button in that editor rather than a second gesture. */
    @Test
    fun theEditorCanReadThePositionAgain() = runComposeUiTest {
        var tapped = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(location = LocationTag(51.5, -0.12), locationName = "Home"),
                    onWhereTapped = { tapped++ },
                )
            }
        }
        onNodeWithContentDescription("Update where you are").performClick()
        onNodeWithText("Find me again").performClick()

        assertEquals(1, tapped)
    }

    @Test
    fun theEditorOffersShopsNearThatPosition() = runComposeUiTest {
        var picked: NearbyPlace? = null
        val place = NearbyPlace(
            name = "Blue Bottle",
            category = SpendingCategory.DINING,
            location = LocationTag(51.5, -0.12),
            visits = 3,
            metresAway = 40.0,
        )
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        location = LocationTag(51.5, -0.12),
                        nearbyPlaces = listOf(place),
                    ),
                    onNearbyPlacePicked = { picked = it },
                )
            }
        }
        onNodeWithContentDescription("Add where you are").assertDoesNotExist()
        onNodeWithContentDescription("Update where you are").performClick()
        onNodeWithText("Nearby").assertIsDisplayed()
        onNodeWithText("Blue Bottle").performClick()

        assertEquals(place, picked)
    }

    /** Nothing to edit or remove without a fix, so a hold there opens nothing. */
    @Test
    fun holdingTheLineDoesNothingBeforeAnythingIsCaptured() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("Where?").performTouchInput { longClick() }

        onNodeWithText("Remove place").assertDoesNotExist()
    }

    @Test
    fun holdingTheLineOffersEditingThePlace() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(location = LocationTag(51.5, -0.12), locationName = "Home"),
                )
            }
        }
        onNodeWithText("Home").performTouchInput { longClick() }
        onNodeWithText("Edit place").performClick()

        onNodeWithText("Where are you?").assertIsDisplayed()
    }

    @Test
    fun holdingTheLineOffersRemovingThePlace() = runComposeUiTest {
        var cleared = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(location = LocationTag(51.5, -0.12), locationName = "Home"),
                    onLocationCleared = { cleared++ },
                )
            }
        }
        onNodeWithText("Home").performTouchInput { longClick() }
        onNodeWithText("Remove place").performClick()

        assertEquals(1, cleared)
    }

    /** A failed read says so on the line, rather than falling back to asking again. */
    @Test
    fun saysSoWhenThePositionCouldNotBeRead() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(locationUnavailable = true))
            }
        }
        onNodeWithText("Location unavailable").assertIsDisplayed()
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

    // --- the split selector -------------------------------------------------

    @Test
    fun theBillIsYoursAloneUntilASplitIsPicked() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState()) }
        }
        onNodeWithText("Just me").assertIsDisplayed()
        onNodeWithText("Split with 2").assertIsDisplayed()
    }

    @Test
    fun tappingASegmentPicksThatManyPeople() = runComposeUiTest {
        var picked: Int? = null
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onSplitCountChange = { picked = it })
            }
        }
        onNodeWithText("Split with 2").performClick()

        assertEquals(2, picked)
    }

    /** The visible segments stop at three; anything larger is one hold away. */
    @Test
    fun holdingTheSelectorOffersTheRestOfTheNumbers() = runComposeUiTest {
        var picked: Int? = null
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onSplitCountChange = { picked = it })
            }
        }
        onNodeWithText("Just me").performTouchInput { longClick() }
        onNodeWithText("Split with how many?").assertIsDisplayed()
        // A number the keypad does not also carry, so this can only be the dialog's chip.
        onNodeWithText("12").performClick()

        assertEquals(12, picked)
    }

    /** A count chosen from the dialog has to survive as a segment, or it looks discarded. */
    @Test
    fun aHeldChoiceBecomesTheSelectedSegment() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(splitEnabled = true, splitWithCount = 7))
            }
        }
        onNodeWithText("Split with 7").assertIsDisplayed()
    }

    /** Income is received whole; there is nobody to share it with. */
    @Test
    fun incomeOffersNoSplit() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(type = TransactionType.CREDIT))
            }
        }
        onNodeWithText("Just me").assertDoesNotExist()
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
