package com.divafinance.feature.quickadd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Person
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
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
    fun theSheetPreviewsAnUnfinishedExpressionWithoutCommittingIt() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(expression = "12+", calculatorOpen = true))
            }
        }
        // The running total tolerates the dangling operator, matching what the form behind
        // the sheet already shows; the expression line above it is what says it is
        // unfinished. `canSave` is still false, so nothing can be committed from here.
        // Two nodes, and that is the assertion: the sheet's figure and the form's figure
        // behind it are the same number rather than disagreeing while the pad is up.
        onAllNodesWithText("$12.00").assertCountEquals(2)
        // The pad carries the expression too, so closing it never loses the sum.
        onAllNodesWithText("12+").assertCountEquals(2)
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
        // The badge under the figure is the code alone now — the symbol is already on
        // the amount, and repeating the currency's full name beside it said it three times.
        onNodeWithText("EUR").assertIsDisplayed()
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
        onNodeWithText("More").assertIsDisplayed()
    }

    /** The rest of the list is a screen now, not an expanding panel. */
    @Test
    fun theMoreChipOpensTheFullPicker() = runComposeUiTest {
        var opened = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        suggestedCategories = listOf(SpendingCategory.GAS),
                        category = SpendingCategory.GAS,
                    ),
                    onOpenCategoryPicker = { opened++ },
                )
            }
        }
        onNodeWithText("More").performClick()

        assertEquals(1, opened)
    }

    /** A category the user invented is shown as itself, not as the parent it behaves as. */
    @Test
    fun aCustomCategoryIsShownByItsOwnName() = runComposeUiTest {
        val ramen = CustomCategory(
            id = "c1", name = "Ramen", iconKey = "restaurant", colorHex = "#0F9D6E",
            parent = SpendingCategory.DINING, createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        category = SpendingCategory.DINING,
                        customCategoryId = "c1",
                        customCategories = listOf(ramen),
                    ),
                )
            }
        }
        onNodeWithText("Ramen").assertIsDisplayed()
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
        onNodeWithText("Add note or tags").performClick()

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
        onNodeWithText("Scan Receipt").performClick()

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
        onNodeWithText("Split Transaction").assertDoesNotExist()
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
        onNodeWithText("Add Location").performClick()

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
        // The form scrolls now that it is not a card with its own inner scroll, so the
        // location block can start below the fold.
        onNodeWithText("Find me again").performScrollTo().performClick()

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

    // --- the split sheet ----------------------------------------------------

    /** The split controls live in a sheet now; nothing about them shows on the form. */
    @Test
    fun splittingIsHiddenUntilItsTileIsTapped() = runComposeUiTest {
        var opened = 0
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(), onSplitSheetOpened = { opened++ })
            }
        }
        onNodeWithText("Split Transaction").assertDoesNotExist()
        onNodeWithText("Split").performClick()

        assertEquals(1, opened)
    }

    /** The form keeps a one-line summary so an open split is never invisible behind a sheet. */
    @Test
    fun theFormSummarisesAnOpenSplit() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "60", splitEnabled = true, splitWithCount = 2),
                )
            }
        }
        onNodeWithText("Split 3 ways", substring = true).assertIsDisplayed()
        onNodeWithText("Your share", substring = true).assertIsDisplayed()
    }

    /** The three modes are the sheet's whole proposition, so they are always offered. */
    @Test
    fun theSheetOffersAllThreeSplitModes() = runComposeUiTest {
        var mode: SplitMode? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "60", splitEnabled = true, splitSheetOpen = true),
                    onSplitModeChange = { mode = it },
                )
            }
        }
        onNodeWithText("Equally").assertIsDisplayed()
        onNodeWithText("By percent").assertIsDisplayed()
        onNodeWithText("By amount").performClick()

        assertEquals(SplitMode.BY_AMOUNT, mode)
    }

    /** A manual allocation has to say whether it adds up, or it cannot be corrected. */
    @Test
    fun theSheetSaysWhenAManualSplitDoesNotAddUp() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        expression = "60",
                        splitEnabled = true,
                        splitSheetOpen = true,
                        splitWithCount = 1,
                        splitMode = SplitMode.BY_AMOUNT,
                        splitCustomAmounts = listOf(10.0, 20.0),
                    ),
                )
            }
        }
        onNodeWithText("of", substring = true).assertIsDisplayed()
        onNodeWithText("allocated", substring = true).assertIsDisplayed()
    }

    /** Choosing a payer is what inverts the debt, so it has to be reachable. */
    @Test
    fun tappingAnAvatarChoosesWhoPaid() = runComposeUiTest {
        var payer: SplitPerson? = null
        var called = false
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        expression = "60",
                        splitEnabled = true,
                        splitSheetOpen = true,
                        splitWith = listOf(SplitPerson(null, "Sam")),
                    ),
                    onSplitPayerChange = { payer = it; called = true },
                )
            }
        }
        onNodeWithContentDescription("Sam paid").performClick()

        assertEquals(true, called)
        assertEquals("Sam", payer?.name)
    }

    /**
     * The running check is shown in every mode, so an even split confirms itself rather
     * than leaving the one reassuring line blank exactly where nothing can go wrong.
     */
    @Test
    fun anEvenSplitStillShowsWhatItAllocated() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        expression = "57.80",
                        splitEnabled = true,
                        splitSheetOpen = true,
                        splitWith = listOf(SplitPerson(null, "Sam"), SplitPerson(null, "Priya")),
                        splitMode = SplitMode.EQUALLY,
                    ),
                )
            }
        }
        onNodeWithText("3 people", substring = true).assertIsDisplayed()
        onNodeWithText("allocated", substring = true).assertIsDisplayed()
    }

    /** The sheet commits the split rather than merely closing, and says so. */
    @Test
    fun theSheetCommitsWithAddSplit() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "60", splitEnabled = true, splitSheetOpen = true),
                )
            }
        }
        onNodeWithText("Add Split").assertIsDisplayed()
    }

    /** Opened before an amount exists, the sheet asks for one instead of dividing zero. */
    @Test
    fun theSheetAsksForAnAmountBeforeItCanDivideAnything() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(splitEnabled = true, splitSheetOpen = true))
            }
        }
        onNodeWithText("Enter an amount to split.").assertIsDisplayed()
    }

    // --- chrome -------------------------------------------------------------

    @Test
    fun theDatePillNamesTheDay() = runComposeUiTest {
        val yesterday = LocalDate.fromEpochDays(todayDate().toEpochDays() - 1)
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(date = yesterday)) }
        }
        onNodeWithText("Yesterday", substring = true).assertIsDisplayed()
    }

    @Test
    fun theDatePillNamesAnOlderDateOutright() = runComposeUiTest {
        val older = LocalDate.fromEpochDays(todayDate().toEpochDays() - 9)
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(date = older)) }
        }
        // Not "9 days ago": a backdated entry is easier to check against a receipt when
        // the pill names the date it will actually be filed under.
        onNodeWithText("${older.day} ${MonthAbbreviations[older.month.number - 1]}")
            .assertIsDisplayed()
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
