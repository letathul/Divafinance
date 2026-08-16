package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteTransactionUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val addUseCase = AddTransactionUseCase(txRepo, cardRepo)
    private val useCase = DeleteTransactionUseCase(txRepo, cardRepo)

    @Test
    fun deletesTransaction() = runTest {
        txRepo.insert(TestData.transaction(id = "tx-1", cardId = null))

        useCase("tx-1")

        assertEquals(0, txRepo.count())
        assertNull(txRepo.getById("tx-1"))
    }

    @Test
    fun reversesCardBalanceOnDebit() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 100.0))
        txRepo.insert(
            TestData.transaction(
                id = "tx-1", cardId = "c1", amount = 50.0, type = TransactionType.DEBIT
            )
        )

        useCase("tx-1")

        assertEquals(50.0, cardRepo.getById("c1")?.currentBalance)
    }

    @Test
    fun doesNotTouchBalanceOnCredit() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 100.0))
        txRepo.insert(
            TestData.transaction(
                id = "tx-1", cardId = "c1", amount = 50.0, type = TransactionType.CREDIT
            )
        )

        useCase("tx-1")

        assertEquals(100.0, cardRepo.getById("c1")?.currentBalance)
    }

    @Test
    fun doesNotTouchBalanceWhenNoCardId() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 100.0))
        txRepo.insert(
            TestData.transaction(id = "tx-1", cardId = null, type = TransactionType.DEBIT)
        )

        useCase("tx-1")

        assertEquals(100.0, cardRepo.getById("c1")?.currentBalance)
    }

    @Test
    fun ignoresUnknownTransaction() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 100.0))

        useCase("nope")

        assertEquals(100.0, cardRepo.getById("c1")?.currentBalance)
    }

    /**
     * The property that makes undo safe: adding then deleting must leave the card exactly
     * where it started, which is what a naive `repository.delete` gets wrong.
     */
    @Test
    fun addThenDeleteLeavesCardBalanceUnchanged() = runTest {
        cardRepo.insert(TestData.card(id = "c1", currentBalance = 250.0))
        val tx = TestData.transaction(
            id = "tx-1", cardId = "c1", amount = 75.5, type = TransactionType.DEBIT
        )

        addUseCase(tx)
        assertEquals(325.5, cardRepo.getById("c1")?.currentBalance)

        useCase("tx-1")

        assertEquals(250.0, cardRepo.getById("c1")?.currentBalance)
        assertEquals(0, txRepo.count())
    }
}
