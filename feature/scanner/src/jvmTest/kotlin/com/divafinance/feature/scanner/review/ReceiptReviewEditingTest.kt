package com.divafinance.feature.scanner.review

import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.scanner.ConfirmReceiptUseCase
import com.divafinance.core.domain.usecase.scanner.GetReceiptUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.model.ReceiptLineItem
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Everything the review form gained beyond merchant/amount/category: an editable date, an
 * expression-capable amount, currency, refund type, notes and the itemised list.
 */
class ReceiptReviewEditingTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val receiptRepo = FakeReceiptRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val rewardRepo = FakeRewardRepository()

    private fun viewModel() = ReceiptReviewViewModel(
        receiptId = "r1",
        getReceipt = GetReceiptUseCase(receiptRepo),
        confirmReceipt = ConfirmReceiptUseCase(
            receiptRepo,
            AddTransactionUseCase(transactionRepo, cardRepo),
        ),
        predictCategory = PredictCategoryUseCase(transactionRepo),
        getAllCards = GetAllCardsUseCase(cardRepo),
        settingsRepository = settingsRepo,
        getBestCard = GetBestCardForCategoryUseCase(cardRepo, rewardRepo, settingsRepo),
    )

    private fun seed(
        totalAmount: Double? = 20.00,
        date: LocalDate? = LocalDate(2024, 6, 15),
        currency: String? = null,
        tax: Double? = null,
        tip: Double? = null,
        items: List<ReceiptLineItem> = emptyList(),
    ) {
        receiptRepo.setReceipts(
            listOf(
                TestData.receipt(id = "r1", merchantName = "Bella Cucina", totalAmount = totalAmount)
                    .copy(date = date, currency = currency, taxAmount = tax, tipAmount = tip),
            ),
        )
        // The list-returning reads leave lineItems empty in the real repository too, so they
        // are seeded through the same door production writes them.
        if (items.isNotEmpty()) receiptRepo.setLineItems("r1", items)
    }

    // ── Date ─────────────────────────────────────────────────────────────────────────

    @Test
    fun theDateIsEditable() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("2024-06-15", vm.uiState.value.dateText)

        vm.onDateChanged(LocalDate(2024, 6, 20))
        vm.onAmountChanged("20.00")
        vm.save()
        advanceUntilIdle()

        assertEquals(LocalDate(2024, 6, 20), transactionRepo.getAll().first().single().date)
    }

    @Test
    fun anUnparseableDateBlocksSavingAndSaysSo() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDateTextChanged("15/06/2024")

        assertTrue(vm.uiState.value.hasDateError)
        assertFalse(vm.uiState.value.canSave)

        vm.onDateTextChanged("2024-06-15")
        assertFalse(vm.uiState.value.hasDateError)
        assertTrue(vm.uiState.value.canSave)
    }

    // ── Amount ───────────────────────────────────────────────────────────────────────

    @Test
    fun theAmountAcceptsAnExpression() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        // A receipt total is exactly the figure people arrive at by adding a tip on.
        vm.onAmountChanged("18.50+3")

        assertEquals(21.50, vm.uiState.value.amount)
    }

    @Test
    fun theAmountStillAcceptsAEuropeanDecimalComma() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAmountChanged("12,50")

        assertEquals(12.50, vm.uiState.value.amount)
    }

    // ── Currency, type, note ─────────────────────────────────────────────────────────

    @Test
    fun theReceiptsOwnCurrencyBeatsTheUsersDefault() = runTest {
        settingsRepo.set("base_currency", "USD")
        seed(currency = "EUR")
        val vm = viewModel()
        advanceUntilIdle()

        // Abroad is exactly when this field matters and exactly when the default is wrong.
        assertEquals("EUR", vm.uiState.value.currency)
    }

    @Test
    fun savesAsARefundWhenAskedTo() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onTypeSelected(TransactionType.CREDIT)
        vm.onCurrencyChanged("gbp")
        vm.onNoteChanged("Returned the shoes")
        vm.save()
        advanceUntilIdle()

        val saved = transactionRepo.getAll().first().single()
        assertEquals(TransactionType.CREDIT, saved.type)
        assertEquals("GBP", saved.currency)
        assertEquals("Returned the shoes", saved.note)
    }

    // ── Line items ───────────────────────────────────────────────────────────────────

    @Test
    fun seedsAndSavesTheItemList() = runTest {
        seed(
            items = listOf(
                ReceiptLineItem(id = "i1", receiptId = "r1", position = 0, description = "Pizza", totalPrice = 12.50),
                ReceiptLineItem(id = "i2", receiptId = "r1", position = 1, description = "Coffee", totalPrice = 7.50),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("Pizza", "Coffee"), vm.uiState.value.items.map { it.description })

        vm.onItemDescriptionChanged("i2", "Espresso")
        vm.save()
        advanceUntilIdle()

        val stored = receiptRepo.lineItemsOf("r1")
        assertEquals(listOf("Pizza", "Espresso"), stored.map { it.description })
    }

    @Test
    fun droppedAndAddedItemsAreRenumbered() = runTest {
        seed(
            items = listOf(
                ReceiptLineItem(id = "i1", receiptId = "r1", position = 0, description = "Pizza", totalPrice = 12.50),
                ReceiptLineItem(id = "i2", receiptId = "r1", position = 1, description = "Coffee", totalPrice = 7.50),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.onItemRemoved("i1")
        vm.onItemAdded()
        val added = vm.uiState.value.items.last().id
        vm.onItemDescriptionChanged(added, "Dessert")
        vm.onItemPriceChanged(added, "6.00")
        vm.save()
        advanceUntilIdle()

        val stored = receiptRepo.lineItemsOf("r1")
        assertEquals(listOf("Coffee", "Dessert"), stored.map { it.description })
        assertEquals(listOf(0, 1), stored.map { it.position })
    }

    @Test
    fun anEmptyRowIsNotSaved() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onItemAdded()
        vm.save()
        advanceUntilIdle()

        assertTrue(receiptRepo.lineItemsOf("r1").isEmpty())
    }

    // ── Reconciliation ───────────────────────────────────────────────────────────────

    @Test
    fun flagsItemsThatDoNotAddUpToTheTotal() = runTest {
        seed(
            totalAmount = 25.00,
            tax = 2.00,
            items = listOf(
                ReceiptLineItem(id = "i1", receiptId = "r1", position = 0, description = "Pizza", totalPrice = 12.50),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(12.50, vm.uiState.value.itemsTotal)
        assertTrue(vm.uiState.value.itemsDisagreeWithTotal)
        // A mismatch is a prompt to look again, not a reason to block the save: a discount
        // line this parser doesn't recognise makes the sums disagree on a fine receipt.
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun doesNotFlagItemsThatReconcile() = runTest {
        seed(
            totalAmount = 22.00,
            tax = 2.00,
            items = listOf(
                ReceiptLineItem(id = "i1", receiptId = "r1", position = 0, description = "Pizza", totalPrice = 12.50),
                ReceiptLineItem(id = "i2", receiptId = "r1", position = 1, description = "Coffee", totalPrice = 7.50),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.itemsDisagreeWithTotal)
    }

    @Test
    fun doesNotFlagAReceiptWithNoItems() = runTest {
        seed()
        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.uiState.value.itemsTotal)
        assertFalse(vm.uiState.value.itemsDisagreeWithTotal)
    }

    // ── Pages ────────────────────────────────────────────────────────────────────────

    @Test
    fun exposesTheExtraPagesOfAMultiPageScan() = runTest {
        receiptRepo.setReceipts(
            listOf(
                TestData.receipt(id = "r1", totalAmount = 10.0)
                    .copy(imagePath = "/p1.jpg", pagePaths = listOf("/p2.jpg", "/p3.jpg")),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("/p2.jpg", "/p3.jpg"), vm.uiState.value.pagePaths)
        assertNotNull(vm.uiState.value.receipt?.imagePath)
    }
}
