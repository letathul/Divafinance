package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.domain.fake.FakeCardRepository
import com.divafinance.core.domain.fake.FakeRewardRepository
import com.divafinance.core.domain.fake.FakeSettingsRepository
import com.divafinance.core.domain.fake.TestData
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetBestCardForCategoryUseCaseTest {

    private val cardRepo = FakeCardRepository()
    private val rewardRepo = FakeRewardRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val useCase = GetBestCardForCategoryUseCase(cardRepo, rewardRepo, settingsRepo)

    @Test
    fun returnsBestCardFirst() = runTest {
        cardRepo.setCards(listOf(
            TestData.card(id = "c1", name = "Basic"),
            TestData.card(id = "c2", name = "Premium"),
        ))
        rewardRepo.setRules(listOf(
            TestData.rule(id = "r1", cardId = "c1", multiplier = 1.0, rewardType = RewardType.CASHBACK),
            TestData.rule(id = "r2", cardId = "c2", multiplier = 5.0, rewardType = RewardType.CASHBACK),
        ))

        val result = useCase(SpendingCategory.DINING, 100.0)

        assertEquals(2, result.size)
        assertEquals("c2", result[0].card.id)
    }

    @Test
    fun usesCustomConversionRates() = runTest {
        settingsRepo.set("points_value", "2.0")

        cardRepo.setCards(listOf(TestData.card(id = "c1")))
        rewardRepo.setRules(listOf(
            TestData.rule(id = "r1", cardId = "c1", multiplier = 3.0, rewardType = RewardType.POINTS)
        ))

        val result = useCase(SpendingCategory.DINING, 100.0)
        // 100 * 3.0 * (2.0 / 100.0) = 6.0
        assertEquals(6.0, result[0].estimatedRewardValue)
    }

    @Test
    fun returnsEmptyForNoCards() = runTest {
        val result = useCase(SpendingCategory.DINING, 100.0)
        assertTrue(result.isEmpty())
    }
}
