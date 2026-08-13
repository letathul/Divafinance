package com.divafinance.core.domain.usecase.graphs

import com.divafinance.core.domain.fake.FakeThresholdRepository
import com.divafinance.core.domain.fake.FakeTransactionRepository
import com.divafinance.core.domain.fake.TestData
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetThresholdGraphDataUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val thresholdRepo = FakeThresholdRepository()
    private val useCase = GetThresholdGraphDataUseCase(txRepo, thresholdRepo)

    @Test
    fun returnsSpendingDataWithThresholds() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 300.0, category = SpendingCategory.DINING),
            TestData.transaction(id = "2", amount = 200.0, category = SpendingCategory.TRAVEL),
        ))
        thresholdRepo.setThresholds(listOf(
            TestData.threshold(id = "t1", category = SpendingCategory.DINING, thresholdPercent = 50.0)
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals(2, result.size)
        assertEquals("DINING", result[0].category)
        assertEquals(300.0, result[0].spending)
        assertTrue(result[0].isOverThreshold)
    }

    @Test
    fun marksUnderThresholdCorrectly() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 100.0, category = SpendingCategory.DINING),
            TestData.transaction(id = "2", amount = 900.0, category = SpendingCategory.TRAVEL),
        ))
        thresholdRepo.setThresholds(listOf(
            TestData.threshold(id = "t1", category = SpendingCategory.DINING, thresholdPercent = 20.0)
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        val dining = result.find { it.category == "DINING" }!!
        assertFalse(dining.isOverThreshold)
    }

    @Test
    fun returnsEmptyForNoTransactions() = runTest {
        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))
        assertTrue(result.isEmpty())
    }

    @Test
    fun sortsBySpendingDescending() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 50.0, category = SpendingCategory.DINING),
            TestData.transaction(id = "2", amount = 200.0, category = SpendingCategory.TRAVEL),
            TestData.transaction(id = "3", amount = 100.0, category = SpendingCategory.GROCERIES),
        ))

        val result = useCase(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))

        assertEquals("TRAVEL", result[0].category)
        assertEquals("GROCERIES", result[1].category)
        assertEquals("DINING", result[2].category)
    }
}
