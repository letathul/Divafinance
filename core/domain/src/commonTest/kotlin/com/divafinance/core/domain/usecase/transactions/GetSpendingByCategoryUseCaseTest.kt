package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSpendingByCategoryUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val useCase = GetSpendingByCategoryUseCase(txRepo)

    @Test
    fun groupsSpendingByCategory() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 30.0, category = SpendingCategory.DINING),
            TestData.transaction(id = "2", amount = 20.0, category = SpendingCategory.DINING),
            TestData.transaction(id = "3", amount = 100.0, category = SpendingCategory.TRAVEL),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals(50.0, result["DINING"])
        assertEquals(100.0, result["TRAVEL"])
    }

    @Test
    fun excludesCreditTransactions() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 100.0, type = TransactionType.DEBIT),
            TestData.transaction(id = "2", amount = 50.0, type = TransactionType.CREDIT),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals(100.0, result["DINING"])
        assertTrue(result.size == 1)
    }

    @Test
    fun returnsEmptyForNoTransactions() = runTest {
        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))
        assertTrue(result.isEmpty())
    }
}
