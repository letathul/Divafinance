package com.divafinance.feature.quickadd

import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.feed.PostTransactionToFeedUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.DeleteTransactionUseCase
import com.divafinance.core.common.Coordinates
import com.divafinance.core.domain.premium.PremiumGate
import com.divafinance.core.domain.usecase.location.SuggestNearbyPlacesUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.domain.usecase.transactions.SuggestMerchantsUseCase
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeLedgerRepository
import com.divafinance.core.testing.fake.FakeFeedRepository
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

    private fun viewModel() = QuickAddViewModel(
        AddTransactionUseCase(txRepo, cardRepo),
        DeleteTransactionUseCase(txRepo, cardRepo, ledgerRepo),
        PredictCategoryUseCase(txRepo),
        SuggestMerchantsUseCase(txRepo),
        GetAllCardsUseCase(cardRepo),
        PostTransactionToFeedUseCase(feedRepo),
        SuggestNearbyPlacesUseCase(txRepo, premiumGate),
        locationSource,
        settingsRepo,
    )

    private fun QuickAddViewModel.type(expression: String) {
        expression.forEach { char ->
            if (char in "+-*/") onOperator(char) else onDigit(char)
        }
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
        vm.save()

        assertNotNull(vm.uiState.value.error)
        assertEquals(0, txRepo.count())
    }

    // --- saving -------------------------------------------------------------

    @Test
    fun savesTheEvaluatedAmount() = runTest {
        val vm = viewModel()
        vm.type("12+8")
        vm.save()

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
        vm.save()

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
        vm.save()

        assertEquals("acc-99", txRepo.getAll().first().single().accountId)
    }

    @Test
    fun usesTheConfiguredBaseCurrency() = runTest {
        settingsRepo.set(UserSettings.KEY_BASE_CURRENCY, "EUR")
        val vm = viewModel()
        vm.type("15")
        vm.save()

        assertEquals("EUR", txRepo.getAll().first().single().currency)
    }

    @Test
    fun preselectsTheDefaultCard() = runTest {
        cardRepo.setCards(listOf(TestData.card(id = "c1")))
        settingsRepo.set(UserSettings.KEY_DEFAULT_CARD_ID, "c1")

        val vm = viewModel()
        vm.onOpened()

        assertEquals("c1", vm.uiState.value.selectedCardId)

        vm.type("15")
        vm.save()
        assertEquals("c1", txRepo.getAll().first().single().cardId)
    }

    @Test
    fun clearsTheFormAfterSaving() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onMerchantChange("Blue Bottle")
        vm.save()

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
        vm.save()

        assertEquals(today(), txRepo.getAll().first().single().date)
    }

    @Test
    fun booksAgainstYesterdayWhenChosen() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onDayChange(QuickAddDay.YESTERDAY)
        vm.save()

        val stored = txRepo.getAll().first().single()
        assertEquals(1, today().toEpochDays() - stored.date.toEpochDays())
    }

    // --- undo ---------------------------------------------------------------

    @Test
    fun reportsTheSaveSoTheSheetCanOfferUndo() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.onCategoryChange(SpendingCategory.DINING)
        vm.save()

        val saved = assertNotNull(vm.saved.value)
        assertEquals(15.0, saved.amount)
        assertEquals(SpendingCategory.DINING, saved.category)
    }

    @Test
    fun undoRemovesTheTransaction() = runTest {
        val vm = viewModel()
        vm.type("15")
        vm.save()
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
        vm.save()
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

    // --- location -----------------------------------------------------------

    @Test
    fun capturesNoLocationUntilTheToggleIsTurnedOn() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.onOpened()
        vm.type("15")
        vm.save()

        assertNull(txRepo.getAll().first().single().location)
    }

    @Test
    fun capturesAndStoresTheLocationOnceEnabled() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Trafalgar Square"

        val vm = viewModel()
        vm.onLocationToggled(enabled = true, permissionGranted = true)

        assertEquals("Trafalgar Square", vm.uiState.value.locationName)

        vm.type("15")
        vm.save()

        val stored = assertNotNull(txRepo.getAll().first().single().location)
        assertEquals(51.5, stored.latitude)
        assertEquals("Trafalgar Square", stored.name)
    }

    /** A denial must leave the switch off rather than on and silently capturing nothing. */
    @Test
    fun turnsTheToggleBackOffWhenPermissionIsRefused() = runTest {
        val vm = viewModel()
        vm.onLocationToggled(enabled = true, permissionGranted = false)

        assertFalse(vm.uiState.value.locationEnabled)
        assertTrue(vm.uiState.value.locationUnavailable)
        assertNull(vm.uiState.value.location)
    }

    @Test
    fun reportsUnavailableWhenNoFixArrives() = runTest {
        locationSource.coordinates = null

        val vm = viewModel()
        vm.onLocationToggled(enabled = true, permissionGranted = true)

        assertTrue(vm.uiState.value.locationUnavailable)
        assertFalse(vm.uiState.value.locationEnabled)
    }

    /** No geocoding backend is common; a position without a name is still worth keeping. */
    @Test
    fun keepsCoordinatesEvenWhenTheyCannotBeNamed() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = null

        val vm = viewModel()
        vm.onLocationToggled(enabled = true, permissionGranted = true)
        vm.type("15")
        vm.save()

        val stored = assertNotNull(txRepo.getAll().first().single().location)
        assertEquals(51.5, stored.latitude)
        assertNull(stored.name)
    }

    @Test
    fun theCapturedNameStaysEditable() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)
        locationSource.description = "Some Street"

        val vm = viewModel()
        vm.onLocationToggled(enabled = true, permissionGranted = true)
        vm.onLocationNameChange("Blue Bottle Coffee")
        vm.type("15")
        vm.save()

        assertEquals(
            "Blue Bottle Coffee",
            txRepo.getAll().first().single().location?.name,
        )
    }

    @Test
    fun turningTheToggleOffDiscardsTheCapture() = runTest {
        locationSource.coordinates = Coordinates(51.5, -0.12)

        val vm = viewModel()
        vm.onLocationToggled(enabled = true, permissionGranted = true)
        assertNotNull(vm.uiState.value.location)

        vm.onLocationToggled(enabled = false, permissionGranted = true)

        assertNull(vm.uiState.value.location)
        assertEquals("", vm.uiState.value.locationName)
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
        vm.save()

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
        vm.onLocationToggled(enabled = true, permissionGranted = true)

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
        vm.onLocationToggled(enabled = true, permissionGranted = true)

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
        vm.onLocationToggled(enabled = true, permissionGranted = true)
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
}

private fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
