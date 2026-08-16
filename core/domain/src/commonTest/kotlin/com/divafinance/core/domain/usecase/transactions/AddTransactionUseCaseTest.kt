package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddTransactionUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val cardRepo = FakeCardRepository()
    private val useCase = AddTransactionUseCase(txRepo, cardRepo)

    @Test
    fun insertsTransaction() = runTest {
        val tx = TestData.transaction(id = "tx-1", cardId = null)

        useCase(tx)

        assertEquals(1, txRepo.count())
        assertEquals("tx-1", txRepo.getById("tx-1")?.id)
    }

    @Test
    fun updatesCardBalanceOnDebit() = runTest {
        val card = TestData.card(id = "c1", currentBalance = 100.0)
        cardRepo.insert(card)

        val tx = TestData.transaction(
            id = "tx-1", cardId = "c1", amount = 50.0, type = TransactionType.DEBIT
        )

        useCase(tx)

        val updatedCard = cardRepo.getById("c1")
        assertEquals(150.0, updatedCard?.currentBalance)
    }

    @Test
    fun doesNotUpdateBalanceOnCredit() = runTest {
        val card = TestData.card(id = "c1", currentBalance = 100.0)
        cardRepo.insert(card)

        val tx = TestData.transaction(
            id = "tx-1", cardId = "c1", amount = 50.0, type = TransactionType.CREDIT
        )

        useCase(tx)

        val updatedCard = cardRepo.getById("c1")
        assertEquals(100.0, updatedCard?.currentBalance)
    }

    @Test
    fun doesNotUpdateBalanceWhenNoCardId() = runTest {
        val tx = TestData.transaction(id = "tx-1", cardId = null, type = TransactionType.DEBIT)

        useCase(tx)

        assertEquals(1, txRepo.count())
    }
}
