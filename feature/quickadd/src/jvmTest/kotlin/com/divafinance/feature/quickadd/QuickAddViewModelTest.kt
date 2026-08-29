package com.divafinance.feature.quickadd

import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.DeleteTransactionUseCase
import com.divafinance.core.common.Coordinates
import com.divafinance.core.domain.premium.PremiumGate
import com.divafinance.core.domain.usecase.location.SuggestNearbyPlacesUseCase
import com.divafinance.core.domain.usecase.people.SaveSplitTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.domain.usecase.transactions.SuggestMerchantsUseCase
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeCustomCategoryRepository
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakePersonRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.FakeFeedRepository
import com.divafinance.core.testing.fake.FakeReceiptFileStore
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickAddViewModelTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val txRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val ledgerRepo = FakeLedgerRepository()
    private val feedRepo = FakeFeedRepository()
    private val settingsRepo = FakeSettingsRepository()
    private var premiumGate = FakePremiumGate(premium = false)
    private var locationSource = FakeLocationSource(coordinates = null)
    private val personRepo = FakePersonRepository()
    private val receiptRepo = FakeReceiptRepository()
    private val receiptFiles = FakeReceiptFileStore()
    private val rewardRepo = FakeRewardRepository()
    private val customCategoryRepo = FakeCustomCategoryRepository()

    private fun viewModel() = QuickAddViewModel(
        AddTransactionUseCase(txRepo, cardRepo),
        DeleteTransactionUseCase(txRepo, cardRepo, ledgerRepo, receiptRepo, receiptFiles),
        PredictCategoryUseCase(txRepo),
        SuggestMerchantsUseCase(txRepo),
        GetAllCardsUseCase(cardRepo),
        PostTransactionToFeedUseCase(feedRepo),
        SuggestNearbyPlacesUseCase(txRepo, premiumGate),
        SaveSplitTransactionUseCase(
            AddTransactionUseCase(txRepo, cardRepo), personRepo, ledgerRepo,
        ),
        GetBestCardForCategoryUseCase(cardRepo, rewardRepo, settingsRepo),
        personRepo,
        ledgerRepo,
        customCategoryRepo,
        locationSource,
        settingsRepo,
    )

    private fun QuickAddViewModel.type(expression: String) {
        expression.forEach { char ->
            when (char) {
                in "+-*/" -> onOperator(char)
                in "()" -> onGroup(char)
                else -> onDigit(char)
            }
        }
    }

    /**
     * Saves the way the screen does: the first save on a fresh entry raises the location
     * consent sheet, and answering it lets the save through. Tests that are not about that
     * gate use this so they read as "save" rather than as two-step choreography.
     */
    private fun QuickAddViewModel.saveNow() {
        save()
        if (uiState.value.locationSheetOpen) onLocationSheetDeclined()
    }

    /** Stands in for the screen's permission launcher answering yes. */
    private fun QuickAddViewModel.grantLocation() = onLocationPermissionResult(granted = true)

    /** The whole first-run path: tap the place line, allow on the consent card. */
    private fun QuickAddViewModel.tapWhereAndAllow() {
        onWhereTapped()
        onLocationAllowed()
        grantLocation()
    }

    // --- amount entry -------------------------------------------------------

    @Test
    fun buildsAnAmountFromTheKeypad() = runTest {
        val vm = viewModel()
        vm.type("12.50")

        assertEquals("12.50", vm.uiState.value.expression)
        assertEquals(12.50, vm.uiState.value.committedAmount)
    }

    @Test
    fun evaluatesArithmeticIntoTheAmount() = runTest {
        val vm = viewModel()
        vm.type("120/3")

        assertEquals(40.0, vm.uiState.value.committedAmount)
    }

    @Test
    fun previewsAnUnfinishedExpression() = runTest {
        val vm = viewModel()
        vm.type("12+")

        assertEquals(12.0, vm.uiState.value.previewAmount)
        assertNull(vm.uiState.value.committedAmount)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun backspaceRemovesTheLastCharacter() = runTest {
        val vm = viewModel()
        vm.type("125")
        vm.onBackspace()

        assertEquals("12", vm.uiState.value.expression)
    }

    @Test
    fun roundsBinaryDriftOutOfTheSavedAmount() = runTest {
        val vm = viewModel()
        vm.type("0.1+0.2")

        assertEquals(0.3, vm.uiState.value.committedAmount)
    }

    // --- typing rules -------------------------------------------------------

    /**
     * The keypad refuses the keystroke rather than letting the expression become one the
     * evaluator has to reject: "12+*" reads as nothing, and a running total that blanks
     * out four keys later cannot say why.
     */
    @Test
    fun reachingForASecondOperatorCorrectsTheFirst() = runTest {
        val vm = viewModel()
        vm.type("12+")
        vm.onOperator('*')

        assertEquals("12*", vm.uiState.value.expression)
        assertEquals(12.0, vm.uiState.value.previewAmount)
    }

    @Test
    fun anOperatorWithNothingToOperateOnIsRefused() = runTest {
        val vm = viewModel()
        vm.onOperator('*')

        assertEquals("", vm.uiState.value.expression)
    }

    @Test
    fun aSecondDecimalPointIsRefused() = runTest {
        val vm = viewModel()
        vm.type("1.5")
        vm.onDigit('.')

        assertEquals("1.5", vm.uiState.value.expression)
    }

    /** Amounts are money; a third decimal would only be rounded away on save. */
    @Test
    fun aTypedAmountStopsAtTwoDecimals() = runTest {
        val vm = viewModel()
        vm.type("1.555")

        assertEquals("1.55", vm.uiState.value.expression)
    }

    @Test
    fun aDigitAfterAGroupMultiplies() = runTest {
        val vm = viewModel()
        vm.type("(3+4)5")

        assertEquals("(3+4)*5", vm.uiState.value.expression)
        assertEquals(35.0, vm.uiState.value.committedAmount)
    }

    // --- equals and history -------------------------------------------------

    @Test
    fun equalsFoldsTheResultBackIn() = runTest {
        val vm = viewModel()
        vm.type("12+8.50")

        assertTrue(vm.onEquals())
        assertEquals("20.50", vm.uiState.value.expression)
    }

    /** Nothing to fold in yet — and the pad turns that false into the feedback `=` gives. */
    @Test
    fun equalsOnAnUnfinishedExpressionChangesNothing() = runTest {
        val vm = viewModel()
        vm.type("12+")

        assertFalse(vm.onEquals())
        assertEquals("12+", vm.uiState.value.expression)
    }

    /** Otherwise "20.50" then "9" reads as "20.509" — the result extended, not replaced. */
    @Test
    fun aDigitAfterEqualsStartsAFreshEntry() = runTest {
        val vm = viewModel()
        vm.type("12+8.50")
        vm.onEquals()
        vm.onDigit('9')

        assertEquals("9", vm.uiState.value.expression)
    }

    /** An operator after `=` is what the fold is for: carry on from the total. */
    @Test
    fun anOperatorAfterEqualsContinuesFromTheResult() = runTest {
        val vm = viewModel()
        vm.type("12+8.50")
        vm.onEquals()
        vm.type("+5")

        assertEquals("20.50+5", vm.uiState.value.expression)
        assertEquals(25.50, vm.uiState.value.committedAmount)
    }

    @Test
    fun equalsRemembersTheSum() = runTest {
        val vm = viewModel()
        vm.type("12+8.50")
        vm.onEquals()

        assertEquals(listOf(CalcEntry("12+8.50", 20.50)), vm.uiState.value.calcHistory)
    }

    /** The point of the history: a drag that takes the pad away must not lose the receipt. */
    @Test
    fun dismissingTheKeypadRemembersTheSum() = runTest {
        val vm = viewModel()
        vm.type("10+20+30")
        vm.onCalculatorDismissed()

        assertEquals(listOf(CalcEntry("10+20+30", 60.0)), vm.uiState.value.calcHistory)
    }

    /** A figure typed straight in is not a calculation, and would only crowd the row. */
    @Test
    fun aBareAmountIsNotRemembered() = runTest {
        val vm = viewModel()
        vm.type("57.80")
        vm.onCalculatorDismissed()

        assertTrue(vm.uiState.value.calcHistory.isEmpty())
    }

    @Test
    fun theSameSumIsRememberedOnceHoweverOftenItIsConfirmed() = runTest {
        val vm = viewModel()
        vm.type("12+8")
        vm.onEquals()
        vm.onCalculatorDismissed()
        vm.onCalculatorDismissed()

        assertEquals(1, vm.uiState.value.calcHistory.size)
    }

    @Test
    fun pickingARememberedSumPutsItBackOnThePad() = runTest {
        val vm = viewModel()
        vm.type("10+20+30")
        vm.onCalculatorDismissed()
        vm.onClear()
        vm.onHistoryEntryPicked(vm.uiState.value.calcHistory.first())

        assertEquals("10+20+30", vm.uiState.value.expression)
        assertEquals(60.0, vm.uiState.value.committedAmount)
    }

    /** A saved entry is finished; the next one is a new context, not a continuation. */
    @Test
    fun savingClearsTheHistory() = runTest {
        val vm = viewModel()
        vm.type("12+8")
        vm.onEquals()
        vm.saveNow()

        assertTrue(vm.uiState.value.calcHistory.isEmpty())
    }

    // --- save gating --------------------------------------------------------

    @Test
    fun cannotSaveAnEmptyAmount() = runTest {
        assertFalse(viewModel().uiState.value.canSave)
    }

    @Test
    fun cannotSaveZero() = runTest {
        val vm = viewModel()
        vm.type("0")

        assertFalse(vm.uiState.value.canSave)
    }

    /** "10-10" evaluates cleanly but is not a transaction. */
    @Test
    fun cannotSaveAnExpressionThatComesToZero() = runTest {
        val vm = viewModel()
        vm.type("10-10")

        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun cannotSaveADivisionByZero() = runTest {
        val vm = viewModel()
        vm.type("10/0")

        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun savingWithoutAnAmountReportsAnErrorRatherThanFailingSilently() = runTest {
        val vm = viewModel()
        vm.saveNow()

        assertNotNull(vm.uiState.value.error)
        assertEquals(0, txRepo.count())
    }

    // --- saving -------------------------------------------------------------

    @Test
    fun savesTheEvaluatedAmount() = runTest {
        val vm = viewModel()
        vm.type("12+8")
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(20.0, stored.amount)
        assertEquals(TransactionType.DEBIT, stored.type)
    }

    @Test
    fun savesTheSelectedCategoryAndMerchant() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onCategoryChange(SpendingCategory.DINING)
        vm.onMerchantChange("Blue Bottle")
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(SpendingCategory.DINING, stored.category)
        assertEquals("Blue Bottle", stored.merchantName)
    }

    @Test
    fun usesTheConfiguredDefaultAccount() = runTest {
        settingsRepo.set(UserSettings.KEY_DEFAULT_ACCOUNT_ID, "acc-99")
        val vm = viewModel()
        vm.onOpened()
        vm.type("15")
        vm.saveNow()

        assertEquals("acc-99", txRepo.getAll().first().single().accountId)
    }

    @Test
    fun usesTheConfiguredBaseCurrency() = runTest {
        settingsRepo.set(UserSettings.KEY_BASE_CURRENCY, "EUR")
        val vm = viewModel()
        vm.onOpened()
        vm.type("15")
        vm.saveNow()

        assertEquals("EUR", txRepo.getAll().first().single().currency)
    }

    /** Denominating one entry abroad must not re-label every report back home. */
    @Test
    fun aPickedCurrencyOverridesTheBaseForThatEntryOnly() = runTest {
        settingsRepo.set(UserSettings.KEY_BASE_CURRENCY, "EUR")
        val vm = viewModel()
        vm.onOpened()
        vm.onCurrencyChange("JPY")
        vm.type("15")
        vm.saveNow()

        assertEquals("JPY", txRepo.getAll().first().single().currency)
        assertEquals("EUR", settingsRepo.get(UserSettings.KEY_BASE_CURRENCY))
    }

    @Test
    fun preselectsTheDefaultCard() = runTest {
        cardRepo.setCards(listOf(TestData.card(id = "c1")))
        settingsRepo.set(UserSettings.KEY_DEFAULT_CARD_ID, "c1")

        val vm = viewModel()
        vm.onOpened()

        assertEquals("c1", vm.uiState.value.selectedCardId)

        vm.type("15")
        vm.saveNow()
        assertEquals("c1", txRepo.getAll().first().single().cardId)
    }

    @Test
    fun clearsTheFormAfterSaving() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onMerchantChange("Blue Bottle")
        vm.saveNow()

        assertEquals("", vm.uiState.value.expression)
        assertEquals("", vm.uiState.value.merchantName)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun switchingToIncomeDropsTheCard() = runTest {
        cardRepo.setCards(listOf(TestData.card(id = "c1")))
        settingsRepo.set(UserSettings.KEY_DEFAULT_CARD_ID, "c1")
        val vm = viewModel()
        vm.onOpened()

        vm.onTypeChange(TransactionType.CREDIT)

        assertNull(vm.uiState.value.selectedCardId)
    }

    @Test
    fun booksAgainstTodayByDefault() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.saveNow()

        assertEquals(today(), txRepo.getAll().first().single().date)
    }

    @Test
    fun booksAgainstYesterdayWhenChosen() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onDateChange(LocalDate.fromEpochDays(today().toEpochDays() - 1))
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(1, today().toEpochDays() - stored.date.toEpochDays())
    }

    // --- undo ---------------------------------------------------------------

    @Test
    fun reportsTheSaveSoTheSheetCanOfferUndo() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onCategoryChange(SpendingCategory.DINING)
        vm.saveNow()

        val saved = assertNotNull(vm.saved.value)
        assertEquals(15.0, saved.amount)
        assertEquals(SpendingCategory.DINING, saved.category)
    }

    @Test
    fun undoRemovesTheTransaction() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.saveNow()
        vm.undo()

        assertEquals(0, txRepo.count())
        assertNull(vm.saved.value)
    }

    /**
     * The reason undo must not call the repository directly: adding a card transaction
     * raises the card balance, so undo has to lower it again.
     */
    @Test
    fun undoRestoresTheCardBalance() = runTest {
        cardRepo.setCards(listOf(TestData.card(id = "c1", currentBalance = 100.0)))
        settingsRepo.set(UserSettings.KEY_DEFAULT_CARD_ID, "c1")
        val vm = viewModel()
        vm.onOpened()

        vm.type("25")
        vm.saveNow()
        assertEquals(125.0, cardRepo.getById("c1")?.currentBalance)

        vm.undo()
        assertEquals(100.0, cardRepo.getById("c1")?.currentBalance)
    }

    // --- category suggestions ----------------------------------------------

    @Test
    fun suggestsMostUsedCategoriesFirst() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.GAS),
                TestData.transaction(id = "2", category = SpendingCategory.GAS),
                TestData.transaction(id = "3", category = SpendingCategory.GAS),
                TestData.transaction(id = "4", category = SpendingCategory.UTILITIES),
                TestData.transaction(id = "5", category = SpendingCategory.UTILITIES),
            )
        )

        val vm = viewModel()
        vm.onOpened()

        val suggestions = vm.uiState.value.suggestedCategories
        assertEquals(SpendingCategory.GAS, suggestions[0])
        assertEquals(SpendingCategory.UTILITIES, suggestions[1])
        assertEquals(SpendingCategory.GAS, vm.uiState.value.category)
    }

    /** A new install has no history, so the row must still be usable. */
    @Test
    fun fallsBackToACommonSetWhenThereIsNoHistory() = runTest {
        val vm = viewModel()
        vm.onOpened()

        assertEquals(5, vm.uiState.value.suggestedCategories.size)
        assertEquals(SpendingCategory.GROCERIES, vm.uiState.value.category)
    }

    /** The whole point of the predictor: a known shop should pick its own category. */
    @Test
    fun typingAKnownMerchantSwitchesThePredictedCategory() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.GROCERIES, merchantName = "Tesco"),
                TestData.transaction(id = "2", category = SpendingCategory.GROCERIES, merchantName = "Tesco"),
                TestData.transaction(id = "3", category = SpendingCategory.GROCERIES, merchantName = "Tesco"),
                TestData.transaction(id = "4", category = SpendingCategory.DINING, merchantName = "Blue Bottle"),
            )
        )

        val vm = viewModel()
        vm.onOpened()
        assertEquals(SpendingCategory.GROCERIES, vm.uiState.value.category)

        vm.onMerchantChange("Blue Bottle")

        assertEquals(SpendingCategory.DINING, vm.uiState.value.category)
    }

    /** Prediction assists; it must never overrule a choice the user already made. */
    @Test
    fun doesNotOverrideACategoryThePersonPickedThemselves() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.DINING, merchantName = "Blue Bottle"),
            )
        )

        val vm = viewModel()
        vm.onOpened()
        vm.onCategoryChange(SpendingCategory.HEALTHCARE)
        vm.onMerchantChange("Blue Bottle")

        assertEquals(SpendingCategory.HEALTHCARE, vm.uiState.value.category)
    }

    @Test
    fun offersRecentMerchantsBeforeAnythingIsTyped() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "1", merchantName = "Tesco"),
                TestData.transaction(id = "2", merchantName = "Tesco"),
                TestData.transaction(id = "3", merchantName = "Costa"),
            )
        )

        val vm = viewModel()
        vm.onOpened()

        assertEquals(listOf("Tesco", "Costa"), vm.uiState.value.merchantSuggestions)
    }

    @Test
    fun narrowsMerchantSuggestionsAsYouType() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "1", merchantName = "Tesco"),
                TestData.transaction(id = "2", merchantName = "Costa"),
            )
        )

        val vm = viewModel()
        vm.onOpened()
        vm.onMerchantChange("cos")

        assertEquals(listOf("Costa"), vm.uiState.value.merchantSuggestions)
    }

    @Test
    fun pickingASuggestionFillsTheMerchantField() = runTest {
        txRepo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.DINING, merchantName = "Blue Bottle"),
            )
        )

        val vm = viewModel()
        vm.onOpened()
        vm.onMerchantSuggestionPicked("Blue Bottle")

        assertEquals("Blue Bottle", vm.uiState.value.merchantName)
        assertEquals(SpendingCategory.DINING, vm.uiState.value.category)
    }

    // --- split ---------------------------------------------------------------

    @Test
    fun splittingIsOffUntilTurnedOn() = runTest {
        val vm = viewModel()
        vm.type("120")

        assertNull(vm.uiState.value.split)
    }

    @Test
    fun dividesTheBillBetweenEveryoneIncludingYou() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("120")
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Alex")

        assertEquals(40.0, vm.uiState.value.splitOwnShare)
        assertEquals(120.0, vm.uiState.value.splitTotal)
    }

    @Test
    fun addsTipOnTopOfTheKeypadAmount() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("100")
        vm.onTipPercentChange(20.0)
        vm.onAddSplitPerson("Sam")

        // 100 + 20 tip, halved.
        assertEquals(120.0, vm.uiState.value.splitTotal)
        assertEquals(60.0, vm.uiState.value.splitOwnShare)
    }

    @Test
    fun doesNotAddTheSamePersonTwice() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("  sam  ")

        assertEquals(1, vm.uiState.value.splitWith.size)
    }

    @Test
    fun ignoresABlankName() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("   ")

        assertTrue(vm.uiState.value.splitWith.isEmpty())
    }

    @Test
    fun removesSomeoneFromTheBill() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onRemoveSplitPerson("Sam")

        assertTrue(vm.uiState.value.splitWith.isEmpty())
    }

    @Test
    fun turningSplittingOffClearsItsState() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onTipPercentChange(15.0)
        vm.onSplitToggled(false)

        assertTrue(vm.uiState.value.splitWith.isEmpty())
        assertEquals(0.0, vm.uiState.value.tipPercent)
        assertNull(vm.uiState.value.split)
    }

    /** The whole point: the card is charged the lot, but only your share is spending. */
    @Test
    fun savesTheFullBillWithOnlyYourShareAsSpending() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("120")
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Alex")
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(120.0, stored.amount)
        assertEquals(80.0, stored.othersShare)
        assertEquals(40.0, stored.amount - stored.othersShare)
    }

    @Test
    fun savingASplitCreatesADebtPerPerson() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("120")
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Alex")
        vm.saveNow()

        val entries = ledgerRepo.getAll().first()
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.amount == 40.0 })
        assertEquals(setOf("Sam", "Alex"), personRepo.getAll().first().map { it.name }.toSet())
    }

    /** Splitting with nobody is just an ordinary transaction. */
    @Test
    fun aSplitWithNoOneElseCreatesNoDebts() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("40")
        vm.saveNow()

        assertEquals(0, ledgerRepo.count())
        assertEquals(0.0, txRepo.getAll().first().single().othersShare)
    }

    @Test
    fun removingThePlaceLeavesTheEntryWithoutOne() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        val vm = viewModel()
        vm.onLocationAllowed()
        vm.onLocationPermissionResult(granted = true)
        assertNotNull(vm.uiState.value.location)

        vm.onLocationCleared()

        assertNull(vm.uiState.value.location)
        assertEquals("", vm.uiState.value.locationName)
        assertEquals(emptyList(), vm.uiState.value.nearbyPlaces)
    }

    // --- splitting by number ------------------------------------------------

    @Test
    fun splittingByNumberDividesWithoutNamingAnyone() = runTest {
        val vm = viewModel()
        vm.onSplitCountChange(2)
        vm.type("120")

        assertEquals(2, vm.uiState.value.splitOthers)
        assertEquals(40.0, vm.uiState.value.splitOwnShare)
        assertEquals(120.0, vm.uiState.value.splitTotal)
    }

    /** Nobody was named, so nobody owes anything — but the share is still not your spend. */
    @Test
    fun anUnnamedSplitKeepsTheOtherSharesOutOfSpendingWithoutCreatingPeople() = runTest {
        val vm = viewModel()
        vm.onSplitCountChange(2)
        vm.type("120")
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(120.0, stored.amount)
        assertEquals(80.0, stored.othersShare)
        assertEquals(0, ledgerRepo.count())
        assertTrue(personRepo.getAll().first().isEmpty())
    }

    @Test
    fun pickingZeroTurnsSplittingOff() = runTest {
        val vm = viewModel()
        vm.onSplitCountChange(3)
        vm.type("120")
        vm.onSplitCountChange(0)

        assertEquals(0, vm.uiState.value.splitOthers)
        assertNull(vm.uiState.value.split)
    }

    /** Naming someone is the more specific statement, so it replaces the bare count. */
    @Test
    fun namingSomeoneTakesOverFromTheNumber() = runTest {
        val vm = viewModel()
        vm.onSplitCountChange(3)
        vm.onAddSplitPerson("Sam")

        assertEquals(1, vm.uiState.value.splitOthers)
        assertEquals(listOf("Sam"), vm.uiState.value.splitWith.map { it.name })
    }

    /** Re-picking the count the names already add up to must not throw them away. */
    @Test
    fun pickingTheCountTheNamesAlreadyMakeKeepsThem() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onSplitCountChange(1)

        assertEquals(listOf("Sam"), vm.uiState.value.splitWith.map { it.name })
    }

    /** An uneven division must still add back up to what was charged. */
    @Test
    fun anUnevenSplitStillReconciles() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("100")
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Alex")
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        val owed = ledgerRepo.getAll().first().sumOf { it.amount }
        assertEquals(100.0, stored.amount)
        assertEquals(stored.othersShare, owed)
        // 33.34 kept, 33.33 owed by each.
        assertEquals(33.34, stored.amount - stored.othersShare)
    }

    // --- location -----------------------------------------------------------

    @Test
    fun capturesNoLocationUntilTheToggleIsTurnedOn() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.onOpened()
        vm.type("15")
        vm.saveNow()

        assertNull(txRepo.getAll().first().single().location)
    }

    @Test
    fun capturesAndStoresTheLocationOncePermissionIsGranted() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Trafalgar Square"

        val vm = viewModel()
        vm.grantLocation()

        assertEquals("Trafalgar Square", vm.uiState.value.locationName)

        vm.type("15")
        vm.saveNow()

        val stored = assertNotNull(txRepo.getAll().first().single().location)
        assertEquals(51.5, stored.latitude)
        assertEquals("Trafalgar Square", stored.name)
    }

    /** A denial must say so rather than leave the sheet looking as if it were locating. */
    @Test
    fun reportsUnavailableWhenPermissionIsRefused() = runTest {
        val vm = viewModel()
        vm.onLocationPermissionResult(granted = false)

        assertFalse(vm.uiState.value.locationPermissionGranted)
        assertFalse(vm.uiState.value.isLocatingNow)
        assertTrue(vm.uiState.value.locationUnavailable)
        assertNull(vm.uiState.value.location)
    }

    @Test
    fun reportsUnavailableWhenNoFixArrives() = runTest {
        locationSource.coordinates = null

        val vm = viewModel()
        vm.grantLocation()

        assertTrue(vm.uiState.value.locationUnavailable)
        assertFalse(vm.uiState.value.isLocatingNow)
    }

    /** No geocoding backend is common; a position without a name is still worth keeping. */
    @Test
    fun keepsCoordinatesEvenWhenTheyCannotBeNamed() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = null

        val vm = viewModel()
        vm.grantLocation()
        vm.type("15")
        vm.saveNow()

        val stored = assertNotNull(txRepo.getAll().first().single().location)
        assertEquals(51.5, stored.latitude)
        assertNull(stored.name)
    }

    @Test
    fun theCapturedNameStaysEditable() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Some Street"

        val vm = viewModel()
        vm.grantLocation()
        vm.onLocationNameChange("Blue Bottle Coffee")
        vm.type("15")
        vm.saveNow()

        assertEquals(
            "Blue Bottle Coffee",
            txRepo.getAll().first().single().location?.name,
        )
    }

    // --- the place line -----------------------------------------------------

    /** Nothing is read until the user has both opted in and answered the OS prompt. */
    @Test
    fun theFirstTapAsksForConsentRatherThanReadingAFix() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.onWhereTapped()

        assertEquals(LocationPrompt.CONSENT, vm.uiState.value.locationPrompt)
        assertNull(vm.uiState.value.location)
        assertEquals(0, vm.uiState.value.permissionRequestNonce)
    }

    /** One card settles both questions: it *is* the rationale the OS prompt cannot give. */
    @Test
    fun allowingOnTheConsentCardLaunchesTheOsPrompt() = runTest {
        val vm = viewModel()
        vm.onWhereTapped()

        vm.onLocationAllowed()

        assertNull(vm.uiState.value.locationPrompt)
        assertEquals(1, vm.uiState.value.permissionRequestNonce)
    }

    @Test
    fun allowingRecordsOnTapAsTheMode() = runTest {
        val vm = viewModel()
        vm.onLocationAllowed()

        assertEquals(
            LocationCaptureMode.ON_TAP.name,
            settingsRepo.get(UserSettings.KEY_LOCATION_CAPTURE_MODE),
        )
    }

    @Test
    fun theAutoCaptureToggleIsRemembered() = runTest {
        val vm = viewModel()
        vm.onAutoCaptureChanged(enabled = true)

        assertEquals(
            LocationCaptureMode.ALWAYS.name,
            settingsRepo.get(UserSettings.KEY_LOCATION_CAPTURE_MODE),
        )
    }

    @Test
    fun theWholeFirstRunPathEndsWithAFix() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Trafalgar Square"

        val vm = viewModel()
        vm.tapWhereAndAllow()

        assertEquals("Trafalgar Square", vm.uiState.value.locationName)
    }

    /** Declining the consent card must leave the entry exactly as it was. */
    @Test
    fun decliningCapturesNothing() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.onWhereTapped()
        vm.onLocationDeclined()

        assertNull(vm.uiState.value.locationPrompt)
        assertNull(vm.uiState.value.location)
        assertEquals(0, vm.uiState.value.permissionRequestNonce)
    }

    /** Once permission is held, the line is a refresh button — no dialogs in the way. */
    @Test
    fun tappingAgainRereadsThePositionWithoutPrompting() = runTest {
        settingsRepo.set(UserSettings.KEY_LOCATION_CAPTURE_MODE, LocationCaptureMode.ON_TAP.name)
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Trafalgar Square"

        val vm = viewModel()
        vm.onOpened()
        vm.grantLocation()

        locationSource.coordinates = Coordinates(48.85, 2.35)
        locationSource.description = "Place de la Concorde"
        vm.onWhereTapped()

        assertNull(vm.uiState.value.locationPrompt)
        assertEquals("Place de la Concorde", vm.uiState.value.locationName)
        assertEquals(48.85, vm.uiState.value.location?.latitude)
    }

    /** A name the user typed is theirs; a re-read updates the fix and leaves it alone. */
    @Test
    fun aRereadKeepsANameTheUserTyped() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Trafalgar Square"

        val vm = viewModel()
        vm.tapWhereAndAllow()
        vm.onLocationNameChange("Blue Bottle Coffee")

        locationSource.description = "Place de la Concorde"
        vm.onWhereTapped()

        assertEquals("Blue Bottle Coffee", vm.uiState.value.locationName)
    }

    /** Losing the fix on a refresh must not throw away the one already captured. */
    @Test
    fun aFailedRereadKeepsTheEarlierFix() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.tapWhereAndAllow()

        locationSource.coordinates = null
        vm.onWhereTapped()

        assertEquals(51.5, vm.uiState.value.location?.latitude)
        assertTrue(vm.uiState.value.locationUnavailable)
    }

    @Test
    fun everyTimeModeAsksForAFixAsSoonAsTheSheetOpens() = runTest {
        settingsRepo.set(UserSettings.KEY_LOCATION_CAPTURE_MODE, LocationCaptureMode.ALWAYS.name)

        val vm = viewModel()
        vm.onOpened()

        assertEquals(LocationCaptureMode.ALWAYS, vm.uiState.value.locationCaptureMode)
        assertEquals(1, vm.uiState.value.permissionRequestNonce)
        assertNull(vm.uiState.value.locationPrompt)
    }

    @Test
    fun onTapModeReadsNothingOnOpen() = runTest {
        settingsRepo.set(UserSettings.KEY_LOCATION_CAPTURE_MODE, LocationCaptureMode.ON_TAP.name)

        val vm = viewModel()
        vm.onOpened()

        assertEquals(0, vm.uiState.value.permissionRequestNonce)
        assertNull(vm.uiState.value.location)
    }

    /**
     * The nonce is what the screen keys its launcher on, so it has to keep climbing. Were
     * it reset with the form, the launcher would see a value it had already handled and
     * the next automatic capture would never fire.
     */
    @Test
    fun resettingTheFormKeepsTheLocationDecisionsAndTheNonce() = runTest {
        settingsRepo.set(UserSettings.KEY_LOCATION_CAPTURE_MODE, LocationCaptureMode.ALWAYS.name)
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.onOpened()
        vm.grantLocation()
        val nonceBefore = vm.uiState.value.permissionRequestNonce

        vm.reset()

        val state = vm.uiState.value
        assertNull(state.location)
        assertEquals(LocationCaptureMode.ALWAYS, state.locationCaptureMode)
        assertTrue(state.locationPermissionGranted)
        assertTrue(state.permissionRequestNonce >= nonceBefore)
    }

    /**
     * A typed place name with no fix must not be saved as (0,0) — that is a real point in
     * the Atlantic and would show up on the spending map.
     */
    @Test
    fun doesNotInventCoordinatesForANameWithoutAFix() = runTest {
        val vm = viewModel()
        vm.onLocationNameChange("Somewhere")
        vm.type("15")
        vm.saveNow()

        assertNull(txRepo.getAll().first().single().location)
    }

    // --- nearby places (premium) --------------------------------------------

    @Test
    fun suggestsNoNearbyShopsWithoutPremium() = runTest {
        premiumGate = FakePremiumGate(premium = false)
        txRepo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "1",
                    merchantName = "Blue Bottle",
                    location = LocationTag(51.5, -0.12),
                )
            )
        )
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.grantLocation()

        assertEquals(emptyList(), vm.uiState.value.nearbyPlaces)
    }

    @Test
    fun suggestsNearbyShopsForPremium() = runTest {
        premiumGate = FakePremiumGate(premium = true)
        txRepo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "1",
                    merchantName = "Blue Bottle",
                    location = LocationTag(51.5, -0.12),
                )
            )
        )
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.grantLocation()

        assertEquals(listOf("Blue Bottle"), vm.uiState.value.nearbyPlaces.map { it.name })
    }

    @Test
    fun pickingANearbyShopFillsBothPlaceAndMerchant() = runTest {
        premiumGate = FakePremiumGate(premium = true)
        txRepo.setTransactions(
            listOf(
                TestData.transaction(
                    id = "1",
                    category = SpendingCategory.DINING,
                    merchantName = "Blue Bottle",
                    location = LocationTag(51.5, -0.12),
                )
            )
        )
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.grantLocation()
        vm.onNearbyPlacePicked(vm.uiState.value.nearbyPlaces.single())

        assertEquals("Blue Bottle", vm.uiState.value.merchantName)
        assertEquals("Blue Bottle", vm.uiState.value.locationName)
        assertEquals(SpendingCategory.DINING, vm.uiState.value.category)
    }

    @Test
    fun neverSuggestsTheSameCategoryTwice() = runTest {
        txRepo.setTransactions(
            listOf(TestData.transaction(id = "1", category = SpendingCategory.GROCERIES))
        )

        val vm = viewModel()
        vm.onOpened()

        val suggestions = vm.uiState.value.suggestedCategories
        assertEquals(suggestions.distinct(), suggestions)
    }

    // --- tags ----------------------------------------------------------------

    @Test
    fun savesTagsAlongsideTheCategory() = runTest {
        val vm = viewModel()
        vm.type("12")
        vm.onAddTag("Work")
        vm.onAddTag("reimbursable")
        vm.saveNow()

        assertEquals(listOf("Work", "reimbursable"), txRepo.getAll().first().single().tags)
    }

    /** "Work" and "work" are one label, not two rows in every report that groups by tag. */
    @Test
    fun deduplicatesTagsIgnoringCase() = runTest {
        val vm = viewModel()
        vm.onAddTag("Work")
        vm.onAddTag("work")

        assertEquals(listOf("Work"), vm.uiState.value.tags)
    }

    @Test
    fun dropsBlankTags() = runTest {
        val vm = viewModel()
        vm.onAddTag("   ")

        assertTrue(vm.uiState.value.tags.isEmpty())
    }

    // --- custom categories ---------------------------------------------------

    /**
     * The point of the parent: a category the user invented still has to land in a bucket
     * every engine already understands.
     */
    @Test
    fun aCustomCategoryIsSavedAsItsParentPlusItsOwnId() = runTest {
        val ramen = CustomCategory(
            id = "c1", name = "Ramen", iconKey = "restaurant", colorHex = "#0F9D6E",
            parent = SpendingCategory.DINING, createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        customCategoryRepo.setCategories(listOf(ramen))

        val vm = viewModel()
        vm.onOpened()
        vm.type("12")
        vm.onCustomCategoryChange(ramen)
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(SpendingCategory.DINING, stored.category)
        assertEquals("c1", stored.customCategoryId)
    }

    @Test
    fun creatingACategorySelectsItStraightAway() = runTest {
        val vm = viewModel()
        vm.onCreateCustomCategory("Ramen", "restaurant", "#0F9D6E", SpendingCategory.DINING)

        assertEquals("Ramen", vm.uiState.value.customCategories.single().name)
        assertEquals(SpendingCategory.DINING, vm.uiState.value.category)
        assertNotNull(vm.uiState.value.customCategoryId)
    }

    /** Going back to a built-in has to drop the custom label, or the entry shows both. */
    @Test
    fun pickingABuiltInClearsTheCustomLabel() = runTest {
        val vm = viewModel()
        vm.onCreateCustomCategory("Ramen", "restaurant", "#0F9D6E", SpendingCategory.DINING)
        vm.onCategoryChange(SpendingCategory.TRAVEL)

        assertNull(vm.uiState.value.customCategoryId)
        assertEquals(SpendingCategory.TRAVEL, vm.uiState.value.category)
    }

    // --- the location gate ---------------------------------------------------

    /** Never on launch: the first save is the first moment the request has any context. */
    @Test
    fun theFirstSaveAsksAboutLocationInsteadOfSaving() = runTest {
        val vm = viewModel()
        vm.onOpened()
        vm.type("12")
        vm.save()

        assertTrue(vm.uiState.value.locationSheetOpen)
        assertTrue(txRepo.getAll().first().isEmpty())
    }

    @Test
    fun decliningLetsTheSaveThrough() = runTest {
        val vm = viewModel()
        vm.onOpened()
        vm.type("12")
        vm.save()
        vm.onLocationSheetDeclined()

        assertEquals(1, txRepo.getAll().first().size)
        assertEquals(false, vm.uiState.value.locationSheetOpen)
    }

    /**
     * An unsolicited question that comes back on the next entry is nagging, so "Not now"
     * is recorded rather than forgotten. Settings can still turn it back on.
     */
    @Test
    fun neverAsksAboutLocationTwice() = runTest {
        val vm = viewModel()
        vm.onOpened()
        vm.type("12")
        vm.save()
        vm.onLocationSheetDeclined()

        vm.type("20")
        vm.save()

        assertEquals(false, vm.uiState.value.locationSheetOpen)
        assertEquals(2, txRepo.getAll().first().size)
        assertEquals(
            LocationCaptureMode.NEVER.name,
            settingsRepo.get(UserSettings.KEY_LOCATION_CAPTURE_MODE),
        )
    }

    /** A platform that cannot do this is an answer already; nothing is interrupted. */
    @Test
    fun doesNotAskWhenThePlatformHasNoLocation() = runTest {
        locationSource = FakeLocationSource(coordinates = null, available = false)
        val vm = viewModel()
        vm.onOpened()
        vm.type("12")
        vm.save()

        assertEquals(false, vm.uiState.value.locationSheetOpen)
        assertEquals(1, txRepo.getAll().first().size)
    }

    // --- who paid ------------------------------------------------------------

    /** The user owes the payer their share, and owes it once. */
    @Test
    fun aBillSomeoneElsePaidCreatesOneBorrowedDebt() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Priya")
        vm.type("60")
        vm.onSplitPayerChange(SplitPerson(null, "Sam"))
        vm.saveNow()

        val entries = ledgerRepo.getAll().first()
        assertEquals(1, entries.size)
        assertEquals(LedgerEntryKind.BORROWED, entries.single().kind)
        assertEquals(20.0, entries.single().amount)
    }

    /**
     * The charge never touched the user's card, so the balance must not move — the whole
     * reason `cardId` is dropped rather than merely ignored.
     */
    @Test
    fun aBillSomeoneElsePaidLeavesTheCardBalanceAlone() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 0.0))
        val vm = viewModel()
        vm.onCardChange("c1")
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("60")
        vm.onSplitPayerChange(SplitPerson(null, "Sam"))
        vm.saveNow()

        assertEquals(0.0, cardRepo.getById("c1")?.currentBalance)
        assertNull(txRepo.getAll().first().single().cardId)
    }

    /** Whoever paid, the user's own consumption is what reaches spending reports. */
    @Test
    fun theUsersShareIsTheSameWhoeverPaid() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("60")
        vm.onSplitPayerChange(SplitPerson(null, "Sam"))
        vm.saveNow()

        val stored = txRepo.getAll().first().single()
        assertEquals(60.0, stored.amount)
        assertEquals(30.0, stored.othersShare)
    }

    // --- split modes ---------------------------------------------------------

    /**
     * An even split is trivially balanced, but the sheet still shows the running check, so
     * it has to report one rather than going null the way the manual modes can.
     */
    @Test
    fun anEvenSplitReportsAllocationTheSameWayTheManualModesDo() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Priya")
        vm.type("57.80")

        val allocation = vm.uiState.value.allocation
        assertEquals(true, allocation?.isBalanced)
        assertEquals(3, allocation?.people)
        assertEquals(57.80, allocation?.target)
    }

    @Test
    fun anExactAmountSplitSavesTheSharesThatWereTyped() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("60")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(0, 45.0)
        vm.onSplitShareChange(1, 15.0)
        vm.saveNow()

        assertEquals(15.0, txRepo.getAll().first().single().othersShare)
        assertEquals(15.0, ledgerRepo.getAll().first().single().amount)
    }

    /** Shares that do not add up have no correct answer, so the entry cannot be saved. */
    @Test
    fun anAmountSplitThatDoesNotAddUpCannotBeSaved() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("60")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(0, 10.0)
        vm.onSplitShareChange(1, 15.0)

        assertNull(vm.uiState.value.split)
        assertEquals(false, vm.uiState.value.canSave)
        assertEquals(false, vm.uiState.value.allocation?.isBalanced)
    }

    /** Percentages allocate through the engine, so the shares still add back to the total. */
    @Test
    fun aPercentSplitAllocatesWithoutLosingAPenny() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Priya")
        vm.type("100")
        vm.onSplitModeChange(SplitMode.BY_PERCENT)

        val split = assertNotNull(vm.uiState.value.split)
        assertEquals(100_00L, split.shares.sumOf { it.amountMinor })
        assertTrue(vm.uiState.value.allocation?.isBalanced == true)
    }

    // --- the running remainder -----------------------------------------------

    /** The figure the user needs while typing is what is *left*, not what is placed. */
    @Test
    fun theAllocationSaysHowMuchIsLeft() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("100")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(0, 20.0)
        vm.onSplitShareChange(1, 65.0)

        val allocation = assertNotNull(vm.uiState.value.allocation)
        assertEquals(15.0, allocation.remaining)
        assertEquals(15.0, allocation.shortfall)
        assertEquals(false, allocation.isOver)
    }

    /**
     * Over and under are the same distance but not the same state: `shortfall` is unsigned
     * for rendering, so `isOver` is what has to carry the direction.
     */
    @Test
    fun theAllocationSaysHowMuchItIsOver() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("100")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(0, 20.0)
        vm.onSplitShareChange(1, 95.0)

        val allocation = assertNotNull(vm.uiState.value.allocation)
        assertEquals(-15.0, allocation.remaining)
        assertEquals(15.0, allocation.shortfall)
        assertEquals(true, allocation.isOver)
    }

    /** The one tap has to leave the bill saveable, or it is not a fix. */
    @Test
    fun assigningTheRemainderBalancesTheBill() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("100")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(1, 65.0)
        vm.onAssignRemainder()

        assertEquals(listOf(35.0, 65.0), vm.uiState.value.splitCustomAmounts)
        assertNotNull(vm.uiState.value.split)
        assertEquals(true, vm.uiState.value.canSave)
    }

    /**
     * The share just edited is the number the user meant — putting the difference back
     * under it would simply undo the edit that created the imbalance.
     */
    @Test
    fun theRemainderGoesToSomeoneOtherThanTheShareJustEdited() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("100")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(0, 20.0)

        assertEquals(1, vm.uiState.value.remainderTargetIndex)
        vm.onAssignRemainder()
        assertEquals(listOf(20.0, 80.0), vm.uiState.value.splitCustomAmounts)
    }

    /** An overshoot bigger than the target's whole share is not offered rather than clamped. */
    @Test
    fun theRemainderIsNotOfferedWhereItWouldGoNegative() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("100")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(0, 10.0)
        vm.onSplitShareChange(1, 130.0)

        assertNull(vm.uiState.value.remainderTargetIndex)
        vm.onAssignRemainder()
        assertEquals(listOf(10.0, 130.0), vm.uiState.value.splitCustomAmounts)
    }

    /** Percentages get the same treatment, on their own scale rather than in money. */
    @Test
    fun aPercentRemainderBalancesToExactlyOneHundred() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onAddSplitPerson("Priya")
        vm.type("57.80")
        vm.onSplitModeChange(SplitMode.BY_PERCENT)
        vm.onSplitShareChange(2, 21.0)

        // An even three-way split is 33.34 / 33.33 / 33.33 — the odd point goes to the
        // payer — so dropping Priya to 21 leaves 12.33 of the scale unassigned.
        val allocation = assertNotNull(vm.uiState.value.allocation)
        assertEquals(true, allocation.isPercent)
        assertEquals(12.33, allocation.shortfall)

        vm.onAssignRemainder()
        assertEquals(100.0, vm.uiState.value.splitPercents.sum())
        assertNotNull(vm.uiState.value.split)
    }

    /** Adding someone changes the length of a positional list, so it cannot be kept. */
    @Test
    fun addingAPersonResetsAManualAllocation() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("60")
        vm.onSplitModeChange(SplitMode.BY_AMOUNT)
        vm.onSplitShareChange(1, 15.0)
        vm.onAddSplitPerson("Priya")

        assertEquals(SplitMode.EQUALLY, vm.uiState.value.splitMode)
        assertTrue(vm.uiState.value.splitCustomAmounts.isEmpty())
        // The index points into a list that no longer exists at that length.
        assertNull(vm.uiState.value.lastEditedShareIndex)
    }

    /** Removing whoever was paying hands the bill back rather than leaving a ghost payer. */
    @Test
    fun removingThePayerHandsTheBillBackToYou() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onSplitPayerChange(SplitPerson(null, "Sam"))
        vm.onRemoveSplitPerson("Sam")

        assertNull(vm.uiState.value.splitPaidBy)
        assertEquals(0, vm.uiState.value.payerIndex)
    }

    /** The steppers run on a bounded scale, so they cannot walk a share off the end of it. */
    @Test
    fun aPercentageCannotExceedTheWholeBill() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.type("60")
        vm.onSplitModeChange(SplitMode.BY_PERCENT)

        vm.onSplitShareChange(0, 140.0)
        assertEquals(100.0, vm.uiState.value.splitPercents[0])

        vm.onSplitShareChange(0, -5.0)
        assertEquals(0.0, vm.uiState.value.splitPercents[0])
    }

    // --- add people ----------------------------------------------------------

    /**
     * Frequent is built out of splits already made, which is what makes handing over an
     * address book optional. A settlement in cash is not a bill they were on, so only
     * ledger entries tied to a transaction count.
     */
    @Test
    fun theRosterCountsOnlyBillsAPersonWasActuallyOn() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "sam", name = "Sam")))
        ledgerRepo.insert(TestData.ledgerEntry(id = "a", personId = "sam", transactionId = "tx-1"))
        ledgerRepo.insert(TestData.ledgerEntry(id = "b", personId = "sam", transactionId = "tx-2"))
        // Two entries from one bill are one split, not two.
        ledgerRepo.insert(TestData.ledgerEntry(id = "c", personId = "sam", transactionId = "tx-2"))
        // A hand repayment carries no transaction, so it is not a bill.
        ledgerRepo.insert(TestData.ledgerEntry(id = "d", personId = "sam", transactionId = null))

        val vm = viewModel()
        vm.onAddPeopleSheetOpened()

        assertEquals(2, vm.uiState.value.peopleSplitCounts["sam"])
    }

    /** Ticking queues; committing is what changes the bill. */
    @Test
    fun tickedPeopleReachTheBillOnlyOnConfirm() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.type("60")
        vm.onAddPeopleSheetOpened()

        vm.onPersonToggled("sam", "Sam")
        vm.onPersonToggled("priya", "Priya")
        assertEquals(2, vm.uiState.value.pendingPeople.size)
        assertTrue(vm.uiState.value.splitWith.isEmpty())

        vm.onConfirmPeople()

        assertEquals(listOf("Sam", "Priya"), vm.uiState.value.splitWith.map { it.name })
        assertTrue(vm.uiState.value.pendingPeople.isEmpty())
        assertFalse(vm.uiState.value.addPeopleSheetOpen)
    }

    /** Ticking twice is a change of mind, not a second copy of the same person. */
    @Test
    fun tickingSomeoneTwiceUnticksThem() = runTest {
        val vm = viewModel()
        vm.onAddPeopleSheetOpened()
        vm.onPersonToggled("sam", "Sam")
        vm.onPersonToggled("sam", "Sam")

        assertTrue(vm.uiState.value.pendingPeople.isEmpty())
    }

    /** The tick is one statement about whether they are on the bill, in both directions. */
    @Test
    fun untickingSomeoneAlreadyOnTheBillTakesThemOff() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddSplitPerson("Sam")
        vm.onAddPeopleSheetOpened()

        vm.onPersonToggled("sam", "Sam")

        assertTrue(vm.uiState.value.splitWith.isEmpty())
        assertTrue(vm.uiState.value.pendingPeople.isEmpty())
    }

    /**
     * Written straight away rather than at save time: the roster is the point, so a person
     * named for an entry that is then abandoned should still be there next time — and
     * `SaveSplitTransactionUseCase` would otherwise create them by name, without the colour.
     */
    @Test
    fun creatingAPersonStoresTheirNameAndColour() = runTest {
        val vm = viewModel()
        vm.onAddPeopleSheetOpened()
        vm.onNewPersonFormToggled()
        vm.onNewPersonNameChange("Maya")
        vm.onNewPersonColorChange("#D1594B")
        vm.onCreatePerson()

        val stored = assertNotNull(personRepo.findByName("Maya"))
        assertEquals("#D1594B", stored.colorHex)
        assertEquals(listOf("Maya"), vm.uiState.value.pendingPeople.map { it.name })
        assertFalse(vm.uiState.value.newPersonFormOpen)
    }

    /** Typing a name the app already knows is picking that person, not making a second one. */
    @Test
    fun creatingAPersonWhoAlreadyExistsRecoloursThemInstead() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "sam", name = "Sam")))
        val vm = viewModel()
        vm.onAddPeopleSheetOpened()
        vm.onNewPersonFormToggled()
        vm.onNewPersonNameChange("sam")
        vm.onNewPersonColorChange("#4A7FD1")
        vm.onCreatePerson()

        assertEquals(1, personRepo.count())
        assertEquals("#4A7FD1", personRepo.getById("sam")?.colorHex)
    }

    /** Importing is not the same act as putting everyone in the address book on the bill. */
    @Test
    fun importedContactsAreOfferedRatherThanAdded() = runTest {
        personRepo.setPeople(listOf(TestData.person(id = "sam", name = "Sam")))
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddPeopleSheetOpened()

        vm.onContactsImported(listOf("Jordan", " sam ", "", "Alex", "Jordan"))

        // Deduplicated, trimmed, and stripped of anyone Frequent already lists.
        assertEquals(listOf("Alex", "Jordan"), vm.uiState.value.importedContacts)
        assertTrue(vm.uiState.value.splitWith.isEmpty())
    }

    /**
     * Both bodies share one sheet, so dismissing the sheet has to clear both flags — an
     * `addPeopleSheetOpen` left set would reopen the split on the roster, and a sheet whose
     * state says "open" after it has animated out cannot be brought back at all.
     */
    @Test
    fun dismissingTheSplitSheetClosesTheRosterWithIt() = runTest {
        val vm = viewModel()
        vm.onSplitSheetOpened()
        vm.onAddPeopleSheetOpened()
        vm.onPersonToggled(null, "Sam")

        vm.onSplitSheetDismissed()

        assertFalse(vm.uiState.value.splitSheetOpen)
        assertFalse(vm.uiState.value.addPeopleSheetOpen)
        assertTrue(vm.uiState.value.pendingPeople.isEmpty())
    }

    /** Closing without committing leaves the bill alone — nothing was added. */
    @Test
    fun dismissingTheRosterDiscardsTheBatch() = runTest {
        val vm = viewModel()
        vm.onSplitToggled(true)
        vm.onAddPeopleSheetOpened()
        vm.onPersonToggled(null, "Sam")
        vm.onAddPeopleSheetDismissed()

        assertTrue(vm.uiState.value.pendingPeople.isEmpty())
        assertTrue(vm.uiState.value.splitWith.isEmpty())
        assertFalse(vm.uiState.value.addPeopleSheetOpen)
    }

    // --- the date ------------------------------------------------------------

    @Test
    fun refusesToBookAnEntryIntoTheFuture() = runTest {
        val vm = viewModel()
        val tomorrow = LocalDate.fromEpochDays(today().toEpochDays() + 1)
        vm.onDateChange(tomorrow)

        assertEquals(today(), vm.uiState.value.date)
    }

    // --- the best card -------------------------------------------------------

    /** Computed from the user's own cards and rules — no network, no bank connection. */
    @Test
    fun rankingTheBestCardUsesOnlyLocalCardsAndRules() = runTest {
        cardRepo.insert(
            TestData.card(id = "c1", name = "Sapphire", creditLimit = 1000.0, currentBalance = 0.0),
        )
        rewardRepo.setRules(
            listOf(
                TestData.rule(
                    id = "r1",
                    cardId = "c1",
                    category = SpendingCategory.DINING,
                    multiplier = 3.0,
                ),
            ),
        )

        val vm = viewModel()
        vm.onOpened()
        vm.type("40")
        vm.onCategoryChange(SpendingCategory.DINING)

        assertEquals("Sapphire", vm.uiState.value.bestCard?.card?.name)
    }

    /** Income is not paid *with* anything, so there is no card to recommend. */
    @Test
    fun offersNoCardForIncome() = runTest {
        cardRepo.insert(
            TestData.card(id = "c1", name = "Sapphire", creditLimit = 1000.0, currentBalance = 0.0),
        )

        val vm = viewModel()
        vm.onOpened()
        vm.onTypeChange(TransactionType.CREDIT)

        assertNull(vm.uiState.value.bestCard)
    }
}

private fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
