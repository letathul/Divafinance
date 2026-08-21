package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PredictCategoryUseCaseTest {

    private val repo = FakeTransactionRepository()
    private val useCase = PredictCategoryUseCase(repo)

    @Test
    fun fillsTheRowFromDefaultsWhenThereIsNoHistory() = runTest {
        val result = useCase()

        assertEquals(5, result.size)
        assertEquals(SpendingCategory.GROCERIES, result.first())
    }

    @Test
    fun alwaysReturnsTheRequestedCount() = runTest {
        repo.setTransactions(
            listOf(TestData.transaction(id = "1", category = SpendingCategory.GAS))
        )

        assertEquals(5, useCase().size)
        assertEquals(3, useCase(limit = 3).size)
    }

    @Test
    fun neverRepeatsACategory() = runTest {
        repo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.GROCERIES),
                TestData.transaction(id = "2", category = SpendingCategory.DINING),
            )
        )

        val result = useCase()
        assertEquals(result.distinct(), result)
    }

    @Test
    fun leadsWithTheMostUsedCategory() = runTest {
        repo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.GAS),
                TestData.transaction(id = "2", category = SpendingCategory.GAS),
                TestData.transaction(id = "3", category = SpendingCategory.GAS),
                TestData.transaction(id = "4", category = SpendingCategory.DINING),
            )
        )

        assertEquals(SpendingCategory.GAS, useCase().first())
    }

    @Test
    fun aKnownMerchantDrivesThePrediction() = runTest {
        repo.setTransactions(
            listOf(
                TestData.transaction(id = "1", category = SpendingCategory.GROCERIES, merchantName = "Tesco"),
                TestData.transaction(id = "2", category = SpendingCategory.GROCERIES, merchantName = "Tesco"),
                TestData.transaction(id = "3", category = SpendingCategory.GROCERIES, merchantName = "Tesco"),
                TestData.transaction(id = "4", category = SpendingCategory.DINING, merchantName = "Blue Bottle"),
            )
        )

        assertEquals(SpendingCategory.DINING, useCase(merchantName = "Blue Bottle").first())
        assertEquals(SpendingCategory.GROCERIES, useCase(merchantName = "Tesco").first())
    }

    @Test
    fun predictionsStillIncludeDefaultsWhenHistoryIsThin() = runTest {
        repo.setTransactions(
            listOf(TestData.transaction(id = "1", category = SpendingCategory.HEALTHCARE))
        )

        val result = useCase()
        assertEquals(SpendingCategory.HEALTHCARE, result.first())
        assertTrue(result.size == 5, "expected the row to be padded, got $result")
    }
}
