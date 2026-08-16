package com.divafinance.core.domain.usecase.location

import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.LocationTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TagTransactionLocationUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val useCase = TagTransactionLocationUseCase(txRepo)

    @Test
    fun tagsTransactionWithLocation() = runTest {
        txRepo.insert(TestData.transaction(id = "tx-1"))

        val location = LocationTag(40.7128, -74.0060, "New York")
        useCase("tx-1", location)

        val updated = txRepo.getById("tx-1")
        assertNotNull(updated?.location)
        assertEquals("New York", updated.location?.name)
        assertEquals(40.7128, updated.location?.latitude)
    }

    @Test
    fun doesNothingForNonexistentTransaction() = runTest {
        val location = LocationTag(0.0, 0.0, "Nowhere")
        useCase("nonexistent", location)

        assertNull(txRepo.getById("nonexistent"))
    }
}
