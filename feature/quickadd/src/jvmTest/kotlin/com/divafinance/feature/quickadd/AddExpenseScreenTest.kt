package com.divafinance.feature.quickadd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.common.ExpressionEvaluator
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Person
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.theme.DivaPlatform
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    /** $57.80 split 34 / 33 / 33 — the state the by-percent artboard is drawn in. */
    private fun percentState() = QuickAddUiState(
        expression = "57.80",
        splitEnabled = true,
        splitSheetOpen = true,
        splitMode = SplitMode.BY_PERCENT,
        splitWith = listOf(SplitPerson(null, "Sam"), SplitPerson(null, "Priya")),
        splitWithCount = 2,
        splitPercents = listOf(34.0, 33.0, 33.0),
    )

    /** The roster, with Sam already on the bill and three people who are not. */
    private fun addPeopleState() = QuickAddUiState(
        expression = "57.80",
        splitEnabled = true,
        splitSheetOpen = true,
        addPeopleSheetOpen = true,
        contactsSupported = true,
        splitWith = listOf(SplitPerson("sam", "Sam")),
        peopleSuggestions = listOf(
            person("Sam").copy(id = "sam"),
            person("Priya").copy(id = "priya"),
            person("Jordan").copy(id = "jordan"),
        ),
        peopleSplitCounts = mapOf("sam" to 6, "priya" to 1),
    )

    /**
     * Mirrors what QuickAddViewModel does, so the rendered total is the real one.
     *
     * Typing goes through `ExpressionEvaluator.append`, the same rule the ViewModel uses —
     * a harness that appended blindly would let these tests pass on key sequences the app
     * refuses, which is the opposite of what they are for.
     */
    private class Harness(state: QuickAddUiState = QuickAddUiState(calculatorOpen = true)) {
        var state = state
            private set

        fun digit(char: Char) = press(char)
        fun operator(symbol: Char) = press(symbol)
        fun group(char: Char) = press(char)
        fun backspace() { state = state.copy(expression = state.expression.dropLast(1)) }
        fun clear() { state = state.copy(expression = "") }
        fun pick(entry: CalcEntry) { state = state.copy(expression = entry.expression) }

        private fun press(key: Char) {
            state = state.copy(expression = ExpressionEvaluator.append(state.expression, key))
        }
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
                    onEquals = { folded++; true },
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

    /** Done is the pad's own commit affordance and is under the thumb, not up in a corner. */
    @Test
    fun doneDismissesTheKeypad() = runComposeUiTest {
        var dismissed = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(expression = "12+8", calculatorOpen = true),
                    onCalculatorDismissed = { dismissed++ },
                )
            }
        }
        onNodeWithText("Done").performClick()

        assertEquals(1, dismissed)
    }

    /** Nothing to show until something has been worked out; an empty row is just a gap. */
    @Test
    fun theHistoryRowIsAbsentUntilThereIsASum() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(calculatorOpen = true)) }
        }
        onAllNodesWithContentDescription("Reuse 12+8.50").assertCountEquals(0)
    }

    /**
     * The whole point of remembering the sum: the pad can be dragged away, and what it was
     * totalling has to be one tap from coming back.
     */
    @Test
    fun aRememberedSumGoesBackOnThePad() = runComposeUiTest {
        val harness = Harness(
            QuickAddUiState(
                calculatorOpen = true,
                calcHistory = listOf(CalcEntry("12+8.50", 20.50)),
            ),
        )
        setContent {
            DivaTheme {
                AddExpenseContent(state = harness.state, onHistoryEntryPicked = harness::pick)
            }
        }
        onNodeWithContentDescription("Reuse 12+8.50").performClick()

        assertEquals("12+8.50", harness.state.expression)
    }

    /**
     * A grey zero means "nothing typed"; it must not also mean "what you typed cannot be
     * read", or the pad looks broken rather than unfinished.
     */
    @Test
    fun anUnreadableExpressionSaysSoRatherThanShowingAGreyZero() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(QuickAddUiState(expression = "(", calculatorOpen = true))
            }
        }
        onNodeWithText("That's not a number yet").assertIsDisplayed()
    }

    @Test
    fun anEmptyPadDoesNotAccuseTheUserOfAnything() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(QuickAddUiState(calculatorOpen = true)) }
        }
        onAllNodesWithText("That's not a number yet").assertCountEquals(0)
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

    /**
     * The roster is drawn once, and the seat is a request rather than a panel.
     *
     * The picker is a whole sheet now, so the dashed seat reports upward and the ViewModel
     * decides — which is the only way the same roster can be reached from the "Paid by" row
     * and from the end of the share list without either one owning it.
     */
    @Test
    fun theAddSeatAsksForThePicker() = runComposeUiTest {
        var opened = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    QuickAddUiState(
                        expression = "60",
                        splitEnabled = true,
                        splitSheetOpen = true,
                        splitWith = listOf(SplitPerson(null, "Sam")),
                        peopleSuggestions = listOf(person("Priya")),
                    ),
                    onAddPeopleSheetOpened = { opened++ },
                )
            }
        }
        // Sam is on the roster once, and nobody else's names are drawn behind it.
        onAllNodesWithContentDescription("Sam paid").assertCountEquals(1)
        onNodeWithText("Priya").assertDoesNotExist()

        onNodeWithContentDescription("Add someone to this bill").performClick()

        assertEquals(1, opened)
        onAllNodesWithContentDescription("Sam paid").assertCountEquals(1)
    }

    // --- by percent ---------------------------------------------------------

    /** The steppers are the quick path, so a press has to be worth exactly one point. */
    @Test
    fun aStepperNudgesAShareByOnePercent() = runComposeUiTest {
        val changes = mutableListOf<Pair<Int, Double>>()
        setContent {
            DivaTheme {
                AddExpenseContent(
                    percentState(),
                    onSplitShareChange = { index, value -> changes += index to value },
                )
            }
        }
        onNodeWithContentDescription("One percent more for Sam").performClick()
        onNodeWithContentDescription("One percent less for Sam").performClick()

        // Sam is index 1 — index 0 is always the user.
        assertEquals(listOf(1 to 34.0, 1 to 32.0), changes)
    }

    /** Nudging from 33 to 60 one press at a time is what typing the figure avoids. */
    @Test
    fun tappingAPercentageLetsItBeTyped() = runComposeUiTest {
        var typed: Double? = null
        setContent {
            DivaTheme {
                AddExpenseContent(
                    percentState(),
                    onSplitShareChange = { _, value -> typed = value },
                )
            }
        }
        onNodeWithContentDescription("Sam's percent").performClick()
        onNodeWithContentDescription("Sam's percent").performTextReplacement("60")

        assertEquals(60.0, typed)
    }

    /** Percentages are not what anyone owes, so the money has to be on the row beside them. */
    @Test
    fun eachPercentageShowsWhatItComesTo() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(percentState()) }
        }
        // 34 / 33 / 33 of $57.80, allocated to the exact cent by the engine.
        onNodeWithText("$19.65").assertIsDisplayed()
        onNodeWithText("$19.08").assertIsDisplayed()
    }

    /** A balanced percentage split says what it comes to; percentages alone do not. */
    @Test
    fun aBalancedPercentSplitReportsTheMoney() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(percentState()) }
        }
        onNodeWithText("100% allocated", substring = true).assertIsDisplayed()
        onNodeWithText("$57.80 of $57.80", substring = true).assertIsDisplayed()
    }

    /** One seat at the end of the list, not one after every person already in it. */
    @Test
    fun theRosterOffersOneSeatHoweverManyPeopleAreOnTheBill() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(percentState()) }
        }
        // "Add person · redistributes remaining %" in this mode, hence the substring.
        onAllNodesWithText("Add person", substring = true).assertCountEquals(1)
    }

    // --- add people ---------------------------------------------------------

    /** Frequent is built from past splits, which is what makes the address book optional. */
    @Test
    fun theRosterSaysHowOftenEachPersonHasBeenSplitWith() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(addPeopleState()) }
        }
        onNodeWithText("Split 6 times before").assertIsDisplayed()
        onNodeWithText("Split once before").assertIsDisplayed()
        onNodeWithText("Not split yet").assertIsDisplayed()
    }

    /** Ticking is a batch: the bill changes length once, when it is committed. */
    @Test
    fun tickingSomeoneReportsThemWithoutTouchingTheBill() = runComposeUiTest {
        var toggled: Pair<String?, String>? = null
        var confirmed = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    addPeopleState().copy(pendingPeople = listOf(SplitPerson("priya", "Priya"))),
                    onPersonToggled = { id, name -> toggled = id to name },
                    onConfirmPeople = { confirmed++ },
                )
            }
        }
        onNodeWithContentDescription("Add Jordan").performClick()
        assertEquals("jordan" to "Jordan", toggled)

        onNodeWithText("Add to Split").performClick()
        assertEquals(1, confirmed)

        // Sam is already on the bill, so his row offers the way back off it.
        onNodeWithContentDescription("Remove Sam").performClick()
        assertEquals("sam" to "Sam", toggled)
    }

    /** The running count is the only thing saying how big the batch has got. */
    @Test
    fun theFooterCountsThePendingBatch() = runComposeUiTest {
        setContent {
            DivaTheme {
                AddExpenseContent(
                    addPeopleState().copy(
                        pendingPeople = listOf(
                            SplitPerson("priya", "Priya"),
                            SplitPerson("jordan", "Jordan"),
                        ),
                    ),
                )
            }
        }
        onNodeWithText("2 selected").assertIsDisplayed()
        onNodeWithText("Add to Split").assertIsEnabled()
    }

    /** Nothing ticked is nothing to add — the button would otherwise close on a no-op. */
    @Test
    fun addToSplitIsDeadUntilSomeoneIsTicked() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(addPeopleState()) }
        }
        onNodeWithText("0 selected").assertIsDisplayed()
        onNodeWithText("Add to Split").assertIsNotEnabled()
    }

    /** A name and a colour, and nothing else — the whole record a split needs. */
    @Test
    fun theNewPersonFormTakesANameAndAColour() = runComposeUiTest {
        var name: String? = null
        var colour: String? = null
        var created = 0
        setContent {
            DivaTheme {
                AddExpenseContent(
                    addPeopleState().copy(newPersonFormOpen = true, newPersonName = "Maya"),
                    onNewPersonNameChange = { name = it },
                    onNewPersonColorChange = { colour = it },
                    onCreatePerson = { created++ },
                )
            }
        }
        onNodeWithContentDescription("Avatar colour 1").performClick()
        assertEquals("#d1594b", colour?.lowercase())

        onNodeWithText("Add Maya").performClick()
        assertEquals(1, created)
        assertNull(name)
    }

    /** Offered only where there is an address book to read. */
    @Test
    fun contactsAreOfferedOnlyWhereTheyExist() = runComposeUiTest {
        setContent {
            DivaTheme { AddExpenseContent(addPeopleState().copy(contactsSupported = false)) }
        }
        onNodeWithText("Import from Contacts").assertDoesNotExist()
    }

    /**
     * The jvm host resolves to MATERIAL, so a sheet only ever tested there is half covered.
     */
    @Test
    fun theRosterRendersOnCupertinoToo() = runComposeUiTest {
        setContent {
            DivaTheme(platform = DivaPlatform.CUPERTINO) {
                AddExpenseContent(addPeopleState())
            }
        }
        onNodeWithText("Add People").assertIsDisplayed()
        onNodeWithText("Split 6 times before").assertIsDisplayed()
        onNodeWithText("Add to Split").assertIsDisplayed()
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
