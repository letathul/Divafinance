package com.divafinance.feature.quickadd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Person
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

/**
 * Drives the stateless sheet body directly. The keypad is the part most worth covering:
 * a wrong amount is the worst thing this screen can do.
 */
@OptIn(ExperimentalTestApi::class)
class AddExpenseScreenTest {

    private fun person(name: String) = Person(
        id = name.lowercase(),
        name = name,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    /** Mirrors what QuickAddViewModel does, so the rendered total is the real one. */
    private class Harness(state: QuickAddUiState = QuickAddUiState(calculatorOpen = true)) {
        var state = state
            private set

        fun digit(char: Char) { state = state.copy(expression = state.expression + char) }
        fun operator(symbol: Char) { state = state.copy(expression = state.expression + symbol) }
        fun group(char: Char) { state = state.copy(expression = state.expression + char) }
        fun backspace() { state = state.copy(expression = state.expression.dropLast(1)) }
        fun clear() { state = state.copy(expression = "") }
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
        onNodeWithText("Save Transaction").assertIsNotEnabled()
    }

    @Test
    fun saveIsEnabledOnceAnAmountIsEntered() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(expression = "12")) }
        }
        onNodeWithText("Save Transaction").assertIsEnabled()
    }

    @Test
    fun rendersTheEvaluatedTotal() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(expression = "12+8.50")) }
        }
        onNodeWithText("$20.50").assertIsDisplayed()
        // The expression stays visible under the total so the sum is checkable.
        onNodeWithText("12+8.50").assertIsDisplayed()
    }

    @Test
    fun showsARunningTotalForAnUnfinishedExpression() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(expression = "12+")) }
        }
        onNodeWithText("$12.00").assertIsDisplayed()
        onNodeWithText("Save Transaction").assertIsNotEnabled()
    }

    // --- the amount sheet ---------------------------------------------------

    /** The pad is behind the amount, which is the only way to it. */
    @Test
    fun theKeypadIsHiddenUntilTheAmountIsTapped() = runComposeUiTest {
        var toggled = 0
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onToggleCalculator = { toggled++ })
            }
        }
        onNodeWithContentDescription("7").assertDoesNotExist()
        onNodeWithContentDescription("Edit the amount").performClick()

        assertEquals(1, toggled)
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
                    onBackspace = harness::backspace,
                )
            }
        }

        onNodeWithContentDescription("5").performClick()
        onNodeWithContentDescription("9").performClick()
        onNodeWithContentDescription("Backspace").performClick()

        assertEquals("5", harness.state.expression)
    }

    /** Grouping is the reason the sheet shows the expression next to its result. */
    @Test
    fun keypadBuildsAGroupedExpression() = runComposeUiTest {
        val harness = Harness()
        setContent {
            DivaTheme {
                AddExpenseContent(
                    state = harness.state,
                    onDigit = harness::digit,
                    onOperator = harness::operator,
                    onGroup = harness::group,
                )
            }
        }

        onNodeWithContentDescription("Open bracket").performClick()
        onNodeWithContentDescription("2").performClick()
        onNodeWithContentDescription("Plus").performClick()
        onNodeWithContentDescription("3").performClick()
        onNodeWithContentDescription("Close bracket").performClick()
        onNodeWithContentDescription("Multiply").performClick()
        onNodeWithContentDescription("4").performClick()

        assertEquals("(2+3)*4", harness.state.expression)
        assertEquals(20.0, harness.state.committedAmount)
    }

    @Test
    fun clearEmptiesTheExpression() = runComposeUiTest {
        val harness = Harness(QuickAddUiState(expression = "123", calculatorOpen = true))
        setContent {
            DivaTheme {
                AddExpenseContent(state = harness.state, onClear = harness::clear)
            }
        }
        onNodeWithContentDescription("Clear").performClick()

        assertEquals("", harness.state.expression)
    }

    /** `=` folds the result back in, so the next key continues from the total. */
    @Test
    fun equalsAsksTheViewModelToFoldTheResultIn() = runComposeUiTest {
        var folded = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "12+8", calculatorOpen = true),
                    onEquals = { folded++ },
                )
            }
        }
        onNodeWithContentDescription("Equals").performClick()

        assertEquals(1, folded)
    }

    /** An unfinished expression has no result to show, and must not show a wrong one. */
    @Test
    fun theSheetShowsNoResultForAnUnfinishedExpression() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(expression = "12+", calculatorOpen = true))
            }
        }
        onNodeWithText("—").assertIsDisplayed()
    }

    // --- currency -----------------------------------------------------------

    @Test
    fun theAmountCarriesTheChosenCurrency() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(expression = "40", currency = "EUR"))
            }
        }
        onNodeWithText("€40.00").assertIsDisplayed()
        onNodeWithText("€  EUR · Euro").assertIsDisplayed()
    }

    @Test
    fun theCurrencyListPicksACurrency() = runComposeUiTest {
        var picked: String? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(currencyPickerOpen = true),
                    onCurrencyChange = { picked = it },
                )
            }
        }
        onNodeWithText("British Pound").performClick()

        assertEquals("GBP", picked)
    }

    // --- categories ---------------------------------------------------------

    @Test
    fun showsSuggestedCategoryChips() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        suggestedCategories = listOf(SpendingCategory.GAS, SpendingCategory.DINING),
                        category = SpendingCategory.GAS,
                    ),
                )
            }
        }
        onNodeWithText("Gas").assertIsDisplayed()
        onNodeWithText("Dining").assertIsDisplayed()
        onNodeWithText("+ New").assertIsDisplayed()
    }

    @Test
    fun theRestOfTheCategoriesAreOneTapAway() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        suggestedCategories = listOf(SpendingCategory.GAS),
                        category = SpendingCategory.GAS,
                        showAllCategories = true,
                    ),
                )
            }
        }
        onNodeWithText("MORE CATEGORIES").assertIsDisplayed()
        onNodeWithText("Healthcare").assertIsDisplayed()
    }

    // --- the detail chips ---------------------------------------------------

    @Test
    fun detailsAreHiddenUntilTheirChipIsTapped() = runComposeUiTest {
        var toggled = 0
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onToggleDetails = { toggled++ })
            }
        }
        onNodeWithText("Merchant").assertDoesNotExist()
        onNodeWithText("Note & tags").performClick()

        assertEquals(1, toggled)
    }

    @Test
    fun theNoteChipRevealsMerchantAndNote() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(showDetails = true)) }
        }
        onNodeWithText("Merchant").assertIsDisplayed()
        onNodeWithText("Note").assertIsDisplayed()
    }

    @Test
    fun theReceiptChipOpensTheScanner() = runComposeUiTest {
        var opened = 0
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onOpenScanner = { opened++ })
            }
        }
        onNodeWithText("Receipt").performClick()

        assertEquals(1, opened)
    }

    /** Income arrives whole; there is nobody to share it with. */
    @Test
    fun incomeCannotBeSplit() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(type = TransactionType.CREDIT, splitEnabled = true),
                )
            }
        }
        onNodeWithText("Just me").assertDoesNotExist()
    }

    // --- location -----------------------------------------------------------

    /** The chip is the whole entry point; nothing about location shows before it. */
    @Test
    fun locationIsHiddenUntilItsChipIsTapped() = runComposeUiTest {
        var toggled = 0
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onLocationChipToggled = { toggled++ })
            }
        }
        onNodeWithText("Use your location on this entry?").assertDoesNotExist()
        onNodeWithText("Location").performClick()

        assertEquals(1, toggled)
    }

    /** One card, not two dialogs: the card is itself the rationale the OS cannot give. */
    @Test
    fun asksForConsentOnceBeforeTheOsPrompt() = runComposeUiTest {
        var allowed = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(showLocation = true),
                    onLocationAllowed = { allowed++ },
                )
            }
        }
        onNodeWithText("Use your location on this entry?").assertIsDisplayed()
        onNodeWithText("Allow").performClick()

        assertEquals(1, allowed)
    }

    @Test
    fun decliningConsentClosesTheBlock() = runComposeUiTest {
        var declined = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(showLocation = true),
                    onLocationDeclined = { declined++ },
                )
            }
        }
        onNodeWithText("Not now").performClick()

        assertEquals(1, declined)
    }

    /** A prompt the ViewModel raised has to surface through the same one card. */
    @Test
    fun aViewModelPromptUsesTheSameCard() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        showLocation = true,
                        locationPermissionGranted = true,
                        locationPrompt = LocationPrompt.CONSENT,
                    ),
                )
            }
        }
        onNodeWithText("Use your location on this entry?").assertIsDisplayed()
    }

    @Test
    fun theCapturedPlaceReplacesTheConsentCard() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        showLocation = true,
                        locationPermissionGranted = true,
                        location = LocationTag(51.5, -0.12),
                        locationName = "Trafalgar Square",
                    ),
                )
            }
        }
        onNodeWithText("Use your location on this entry?").assertDoesNotExist()
        // Twice over: named on the row, and editable in the field under it.
        onAllNodesWithText("Trafalgar Square").assertCountEquals(2)
    }

    @Test
    fun theBlockOffersShopsNearThatPosition() = runComposeUiTest {
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
                        showLocation = true,
                        locationPermissionGranted = true,
                        location = LocationTag(51.5, -0.12),
                        nearbyPlaces = listOf(place),
                    ),
                    onNearbyPlacePicked = { picked = it },
                )
            }
        }
        onNodeWithText("NEARBY").assertIsDisplayed()
        onNodeWithText("Blue Bottle").performClick()

        assertEquals(place, picked)
    }

    @Test
    fun theBlockCanReadThePositionAgain() = runComposeUiTest {
        var tapped = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        showLocation = true,
                        locationPermissionGranted = true,
                        location = LocationTag(51.5, -0.12),
                        locationName = "Home",
                    ),
                    onFindMeAgain = { tapped++ },
                )
            }
        }
        onNodeWithText("Find me again").performClick()

        assertEquals(1, tapped)
    }

    @Test
    fun thePlaceCanBeRemoved() = runComposeUiTest {
        var cleared = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        showLocation = true,
                        locationPermissionGranted = true,
                        location = LocationTag(51.5, -0.12),
                        locationName = "Home",
                    ),
                    onLocationCleared = { cleared++ },
                )
            }
        }
        onNodeWithText("Remove").performClick()

        assertEquals(1, cleared)
    }

    /** A failed read says so, rather than falling back to asking again. */
    @Test
    fun saysSoWhenThePositionCouldNotBeRead() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        showLocation = true,
                        locationPermissionGranted = true,
                        locationUnavailable = true,
                    ),
                )
            }
        }
        onNodeWithText("Location unavailable").assertIsDisplayed()
    }

    /** Auto-capture is offered only once there is something to auto-capture. */
    @Test
    fun theCapturedPlaceOffersAutoCaptureNextTime() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        showLocation = true,
                        locationPermissionGranted = true,
                        location = LocationTag(51.5, -0.12),
                        locationName = "Home",
                    ),
                )
            }
        }
        onNodeWithText("Capture location next time").assertIsDisplayed()
    }

    // --- the split block ----------------------------------------------------

    /** The count selector is inside the block, which the Split chip opens. */
    @Test
    fun splittingIsHiddenUntilItsChipIsTapped() = runComposeUiTest {
        var enabled: Boolean? = null
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onSplitToggled = { enabled = it })
            }
        }
        onNodeWithText("Just me").assertDoesNotExist()
        onNodeWithText("Split").performClick()

        assertEquals(true, enabled)
    }

    @Test
    fun theBillIsYoursAloneUntilACountIsPicked() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(splitEnabled = true)) }
        }
        onNodeWithText("Just me").assertIsDisplayed()
        onNodeWithText("Split with 2").assertIsDisplayed()
    }

    @Test
    fun tappingASegmentPicksThatManyPeople() = runComposeUiTest {
        var picked: Int? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(splitEnabled = true),
                    onSplitCountChange = { picked = it },
                )
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
                AddExpenseContent(
                    QuickAddUiState(splitEnabled = true),
                    onSplitCountChange = { picked = it },
                )
            }
        }
        onNodeWithText("Just me").performTouchInput { longClick() }
        onNodeWithText("Split with how many?").assertIsDisplayed()
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

    /** Tapping someone already on the bill is what takes them off again. */
    @Test
    fun tappingAnAvatarTakesThatPersonOffTheBill() = runComposeUiTest {
        var removed: String? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        splitEnabled = true,
                        splitWith = listOf(SplitPerson(null, "Liz Ryan")),
                    ),
                    onRemoveSplitPerson = { removed = it },
                )
            }
        }
        onNodeWithContentDescription("Take Liz Ryan off this bill").performClick()

        assertEquals("Liz Ryan", removed)
    }

    @Test
    fun theAddButtonOffersPeopleAlreadyKnown() = runComposeUiTest {
        var added: String? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        splitEnabled = true,
                        peopleSuggestions = listOf(person("Maya Kaur")),
                    ),
                    onAddSplitPerson = { added = it },
                )
            }
        }
        onNodeWithText("Maya Kaur").assertDoesNotExist()
        onNodeWithContentDescription("Add someone to this bill").performClick()
        onNodeWithText("Maya Kaur").performClick()

        assertEquals("Maya Kaur", added)
    }

    @Test
    fun theSplitBreakdownShowsWhatEachPersonOwes() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        expression = "30",
                        splitEnabled = true,
                        splitWith = listOf(SplitPerson(null, "Liz")),
                    ),
                )
            }
        }
        onNodeWithText("Your share").performScrollTo().assertIsDisplayed()
        onNodeWithText("Total charged").assertIsDisplayed()
    }

    // --- chrome -------------------------------------------------------------

    @Test
    fun theDayChipNamesTheDay() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(day = QuickAddDay.YESTERDAY)) }
        }
        onNodeWithText("Yesterday", substring = true).assertIsDisplayed()
    }

    @Test
    fun closingTheScreenIsOneTap() = runComposeUiTest {
        var dismissed = 0
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(), onDismiss = { dismissed++ }) }
        }
        onNodeWithContentDescription("Cancel").performClick()

        assertEquals(1, dismissed)
    }

    @Test
    fun showsAnErrorWhenSaveIsRejected() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(error = "Enter an amount greater than zero"))
            }
        }
        onNodeWithText("Enter an amount greater than zero").assertIsDisplayed()
    }
}
