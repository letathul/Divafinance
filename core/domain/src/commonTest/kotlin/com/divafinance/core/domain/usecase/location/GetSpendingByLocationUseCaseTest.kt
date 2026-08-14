package com.divafinance.core.domain.usecase.location

import com.divafinance.core.domain.fake.FakeTransactionRepository
import com.divafinance.core.domain.fake.TestData
import com.divafinance.core.model.LocationTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSpendingByLocationUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val useCase = GetSpendingByLocationUseCase(txRepo)

    @Test
    fun groupsTransactionsByLocationName() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", location = LocationTag(40.7, -74.0, "NYC")),
            TestData.transaction(id = "2", location = LocationTag(40.7, -74.0, "NYC")),
            TestData.transaction(id = "3", location = LocationTag(34.0, -118.2, "LA")),
        ))

        val result = useCase()

        assertEquals(2, result.size)
        assertEquals(2, result["NYC"]?.size)
        assertEquals(1, result["LA"]?.size)
    }

    @Test
    fun usesUnknownForNullLocationName() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", location = LocationTag(0.0, 0.0, null)),
        ))

        val result = useCase()

        assertEquals(1, result["Unknown"]?.size)
    }

    @Test
    fun returnsEmptyForNoLocationTransactions() = runTest {
        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", location = null),
        ))

        val result = useCase()

        assertTrue(result.isEmpty())
    }
}
