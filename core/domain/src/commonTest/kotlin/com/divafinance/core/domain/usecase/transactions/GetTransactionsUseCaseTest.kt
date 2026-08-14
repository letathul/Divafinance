package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.domain.fake.FakeTransactionRepository
import com.divafinance.core.domain.fake.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetTransactionsUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val useCase = GetTransactionsUseCase(txRepo)

    @Test
    fun returnsAllTransactions() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "tx-1"),
            TestData.transaction(id = "tx-2"),
        ))

        val result = useCase().first()

        assertEquals(2, result.size)
    }

    @Test
    fun returnsEmptyWhenNoTransactions() = runTest {
        val result = useCase().first()
        assertTrue(result.isEmpty())
    }
}
