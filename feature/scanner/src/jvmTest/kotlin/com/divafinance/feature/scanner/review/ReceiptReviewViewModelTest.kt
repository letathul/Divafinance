package com.divafinance.feature.scanner.review

import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.scanner.ConfirmReceiptUseCase
import com.divafinance.core.domain.usecase.scanner.GetReceiptUseCase
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.domain.usecase.transactions.PredictCategoryUseCase
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.feature.scanner.FailingTransactionRepository
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

class ReceiptReviewViewModelTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val receiptRepo = FakeReceiptRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val settingsRepo = FakeSettingsRepository()

    private fun viewModel(
        receiptId: String = "r1",
        writeTo: TransactionRepository = transactionRepo,
    ) = ReceiptReviewViewModel(
        receiptId = receiptId,
        getReceipt = GetReceiptUseCase(receiptRepo),
        confirmReceipt = ConfirmReceiptUseCase(
            receiptRepo,
            AddTransactionUseCase(writeTo, cardRepo),
        ),
        predictCategory = PredictCategoryUseCase(transactionRepo),
        getAllCards = GetAllCardsUseCase(cardRepo),
        settingsRepository = settingsRepo,
    )

    @Test
    fun seedsTheFormFromTheStoredReceipt() = runTest {
        receiptRepo.setReceipts(
            listOf(
                TestData.receipt(
                    id = "r1",
                    merchantName = "Blue Bottle",
                    totalAmount = 6.25,
                    date = LocalDate(2024, 6, 15),
                ),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Blue Bottle", state.merchantName)
        assertEquals(6.25, state.amount)
        assertEquals(LocalDate(2024, 6, 15), state.date)
        assertTrue(state.suggestedCategories.isNotEmpty())
    }

    @Test
    fun reportsAReceiptThatIsNoLongerThere() = runTest {
        val vm = viewModel(receiptId = "missing")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun cannotSaveWithoutAUsableAmount() = runTest {
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1", totalAmount = null)))
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canSave)

        vm.onAmountChanged("not a number")
        assertFalse(vm.uiState.value.canSave)

        vm.onAmountChanged("0")
        assertFalse(vm.uiState.value.canSave)

        vm.onAmountChanged("12.30")
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun savingWritesBothSidesOfTheLink() = runTest {
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1", totalAmount = 6.25)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        val transaction = transactionRepo.getAll().first().single()
        val receipt = receiptRepo.getReceipts().single()
        assertEquals("r1", transaction.receiptId)
        assertEquals(transaction.id, receipt.transactionId)
        assertEquals(ReceiptStatus.PROCESSED, receipt.status)
        assertEquals(transaction.id, vm.saved.value)
    }

    @Test
    fun savingAppliesTheUsersCorrections() = runTest {
        receiptRepo.setReceipts(
            listOf(TestData.receipt(id = "r1", merchantName = "STARBUKS", totalAmount = 5.0)),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.onMerchantChanged("Starbucks")
        vm.onAmountChanged("5.50")
        vm.save()
        advanceUntilIdle()

        val transaction = transactionRepo.getAll().first().single()
        assertEquals("Starbucks", transaction.merchantName)
        assertEquals(5.50, transaction.amount)
    }

    @Test
    fun savingUpdatesTheCardBalance() = runTest {
        cardRepo.setCards(listOf(TestData.card(id = "card-1", currentBalance = 100.0)))
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1", totalAmount = 25.0)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCardSelected("card-1")
        vm.save()
        advanceUntilIdle()

        assertEquals(125.0, cardRepo.getById("card-1")?.currentBalance)
    }

    @Test
    fun resumingAPendingReceiptMatchesAFreshScan() = runTest {
        // The stored row is the only input, so "resume" and "review" are the same path.
        receiptRepo.setReceipts(
            listOf(
                TestData.receipt(
                    id = "r1",
                    merchantName = "Corner Deli",
                    totalAmount = 9.0,
                    status = ReceiptStatus.PENDING,
                ),
            ),
        )

        val first = viewModel()
        advanceUntilIdle()
        val resumed = viewModel()
        advanceUntilIdle()

        assertEquals(first.uiState.value.merchantName, resumed.uiState.value.merchantName)
        assertEquals(first.uiState.value.amount, resumed.uiState.value.amount)
        assertEquals(first.uiState.value.date, resumed.uiState.value.date)
    }

    @Test
    fun aFailedSaveClearsIsSavingSoTheUserCanRetry() = runTest {
        receiptRepo.setReceipts(listOf(TestData.receipt(id = "r1", totalAmount = 6.25)))
        val vm = viewModel(writeTo = FailingTransactionRepository())
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSaving)
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.saved.value)
        // The receipt stays resumable rather than being marked processed against nothing.
        assertEquals(ReceiptStatus.PENDING, receiptRepo.getReceipts().single().status)
    }
}
