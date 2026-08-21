package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetHighImpactTransactionsUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val useCase = GetHighImpactTransactionsUseCase(txRepo, settingsRepo)

    @Test
    fun filtersTransactionsAboveDefaultThreshold() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 150.0),
            TestData.transaction(id = "2", amount = 50.0),
            TestData.transaction(id = "3", amount = 200.0),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals(2, result.size)
        assertEquals("3", result[0].id)
        assertEquals("1", result[1].id)
    }

    @Test
    fun usesCustomThresholdFromSettings() = runTest {
        settingsRepo.set("impact_threshold", "75.0")

        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 80.0),
            TestData.transaction(id = "2", amount = 50.0),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun excludesCreditTransactions() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 500.0, type = TransactionType.CREDIT),
            TestData.transaction(id = "2", amount = 200.0, type = TransactionType.DEBIT),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals(1, result.size)
        assertEquals("2", result[0].id)
    }

    @Test
    fun sortsByAmountDescending() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 100.0),
            TestData.transaction(id = "2", amount = 300.0),
            TestData.transaction(id = "3", amount = 200.0),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals("2", result[0].id)
        assertEquals("3", result[1].id)
        assertEquals("1", result[2].id)
    }
}
