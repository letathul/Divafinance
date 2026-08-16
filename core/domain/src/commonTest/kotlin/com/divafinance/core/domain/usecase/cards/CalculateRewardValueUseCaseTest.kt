package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.RewardType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateRewardValueUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val useCase = CalculateRewardValueUseCase(settingsRepo)

    @Test
    fun calculatesCashbackDirectly() = runTest {
        val rule = TestData.rule(multiplier = 0.03, rewardType = RewardType.CASHBACK)
        val result = useCase(rule, 200.0)
        assertEquals(6.0, result)
    }

    @Test
    fun calculatesPointsWithConversion() = runTest {
        settingsRepo.set("points_value", "1.5")
        val rule = TestData.rule(multiplier = 2.0, rewardType = RewardType.POINTS)
        val result = useCase(rule, 100.0)
        // 100 * 2.0 * (1.5 / 100) = 3.0
        assertEquals(3.0, result)
    }

    @Test
    fun calculatesMilesWithConversion() = runTest {
        settingsRepo.set("miles_value", "2.0")
        val rule = TestData.rule(multiplier = 3.0, rewardType = RewardType.MILES)
        val result = useCase(rule, 100.0)
        // 100 * 3.0 * (2.0 / 100) = 6.0
        assertEquals(6.0, result)
    }

    @Test
    fun respectsCapAmount() = runTest {
        val rule = TestData.rule(multiplier = 0.05, rewardType = RewardType.CASHBACK, capAmount = 50.0)
        val result = useCase(rule, 200.0)
        // min(200, 50) * 0.05 = 2.5
        assertEquals(2.5, result)
    }

    @Test
    fun usesDefaultConversionRates() = runTest {
        val rule = TestData.rule(multiplier = 1.0, rewardType = RewardType.POINTS)
        val result = useCase(rule, 100.0)
        // 100 * 1.0 * (1.0 / 100) = 1.0
        assertEquals(1.0, result)
    }
}
