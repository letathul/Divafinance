package com.divafinance.core.domain.usecase.graphs

import com.divafinance.core.domain.fake.FakeThresholdRepository
import com.divafinance.core.domain.fake.TestData
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigureThresholdUseCaseTest {

    private val thresholdRepo = FakeThresholdRepository()
    private val useCase = ConfigureThresholdUseCase(thresholdRepo)

    @Test
    fun insertsNewThreshold() = runTest {
        val threshold = TestData.threshold(id = "t1")

        useCase.upsert(threshold)

        assertEquals(1, thresholdRepo.getThresholds().size)
    }

    @Test
    fun updatesExistingThreshold() = runTest {
        thresholdRepo.insert(TestData.threshold(id = "t1", thresholdPercent = 30.0))

        val updated = TestData.threshold(id = "new-id", category = SpendingCategory.DINING, thresholdPercent = 50.0)
        useCase.upsert(updated)

        val thresholds = thresholdRepo.getThresholds()
        assertEquals(1, thresholds.size)
        assertEquals(50.0, thresholds[0].thresholdPercent)
    }

    @Test
    fun deletesThreshold() = runTest {
        thresholdRepo.insert(TestData.threshold(id = "t1"))

        useCase.delete("t1")

        assertTrue(thresholdRepo.getThresholds().isEmpty())
    }
}
