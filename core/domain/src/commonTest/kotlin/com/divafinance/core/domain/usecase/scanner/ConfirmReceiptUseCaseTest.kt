package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfirmReceiptUseCaseTest {

    private val receiptRepo = FakeReceiptRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val useCase = ConfirmReceiptUseCase(
        receiptRepo,
        AddTransactionUseCase(transactionRepo, cardRepo),
    )

    @Test
    fun savesTheTransactionPointingAtTheReceipt() = runTest {
        val receipt = TestData.receipt(id = "r1")
        receiptRepo.setReceipts(listOf(receipt))

        useCase(receipt, TestData.transaction(id = "t1").copy(receiptId = "r1"))

        val saved = transactionRepo.getAll().first().single()
        assertEquals("t1", saved.id)
        assertEquals("r1", saved.receiptId)
    }

    @Test
    fun linksTheReceiptBackAndMarksItProcessed() = runTest {
        val receipt = TestData.receipt(id = "r1", status = ReceiptStatus.PENDING)
        receiptRepo.setReceipts(listOf(receipt))

        useCase(receipt, TestData.transaction(id = "t1").copy(receiptId = "r1"))

        val stored = receiptRepo.getReceipts().single()
        assertEquals("t1", stored.transactionId)
        assertEquals(ReceiptStatus.PROCESSED, stored.status)
    }

    @Test
    fun carriesTheReviewedValuesOntoTheReceipt() = runTest {
        // The user corrected the OCR on the review screen; the stored row should agree with
        // the transaction rather than keep the misread.
        val receipt = TestData.receipt(id = "r1", merchantName = "STARBUKS", totalAmount = 5.0)
        receiptRepo.setReceipts(listOf(receipt))

        val transaction = TestData.transaction(id = "t1", merchantName = "Starbucks", amount = 5.5)
            .copy(receiptId = "r1")
        useCase(receipt, transaction)

        val stored = receiptRepo.getReceipts().single()
        assertEquals("Starbucks", stored.merchantName)
        assertEquals(5.5, stored.totalAmount)
        assertEquals(transaction.date, stored.date)
    }

    @Test
    fun updatesTheCardBalance() = runTest {
        // Proves this went through AddTransactionUseCase rather than the repository directly —
        // inserting straight through the repo would leave the card permanently understated.
        cardRepo.setCards(listOf(TestData.card(id = "card-1", currentBalance = 100.0)))
        val receipt = TestData.receipt(id = "r1")
        receiptRepo.setReceipts(listOf(receipt))

        useCase(receipt, TestData.transaction(cardId = "card-1", amount = 25.0).copy(receiptId = "r1"))

        assertEquals(125.0, cardRepo.getById("card-1")?.currentBalance)
    }
}
