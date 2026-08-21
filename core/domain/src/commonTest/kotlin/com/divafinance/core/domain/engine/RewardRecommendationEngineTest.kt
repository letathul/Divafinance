package com.divafinance.core.domain.engine

import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardRecommendationEngineTest {

    private val engine = RewardRecommendationEngine(pointsValueCents = 1.0, milesValueCents = 1.5)

    @Test
    fun ranksCardsByRewardValue() {
        val card1 = TestData.card(
            id = "c1", name = "Low Reward",
            rewardRules = listOf(TestData.rule(id = "r1", cardId = "c1", multiplier = 1.0))
        )
        val card2 = TestData.card(
            id = "c2", name = "High Reward",
            rewardRules = listOf(TestData.rule(id = "r2", cardId = "c2", multiplier = 5.0))
        )

        val result = engine.rankCards(listOf(card1, card2), SpendingCategory.DINING, 100.0)

        assertEquals(2, result.size)
        assertEquals("c2", result[0].card.id)
        assertEquals("c1", result[1].card.id)
        assertTrue(result[0].estimatedRewardValue > result[1].estimatedRewardValue)
    }

    @Test
    fun filtersInactiveCards() {
        val active = TestData.card(
            id = "c1", isActive = true,
            rewardRules = listOf(TestData.rule(id = "r1", cardId = "c1"))
        )
        val inactive = TestData.card(
            id = "c2", isActive = false,
            rewardRules = listOf(TestData.rule(id = "r2", cardId = "c2"))
        )

        val result = engine.rankCards(listOf(active, inactive), SpendingCategory.DINING, 100.0)

        assertEquals(1, result.size)
        assertEquals("c1", result[0].card.id)
    }

    @Test
    fun filtersCardsByAvailableCredit() {
        val hasCredit = TestData.card(id = "c1", creditLimit = 5000.0, currentBalance = 1000.0,
            rewardRules = listOf(TestData.rule(id = "r1", cardId = "c1")))
        val noCredit = TestData.card(id = "c2", creditLimit = 100.0, currentBalance = 99.0,
            rewardRules = listOf(TestData.rule(id = "r2", cardId = "c2")))

        val result = engine.rankCards(listOf(hasCredit, noCredit), SpendingCategory.DINING, 50.0)

        assertEquals(1, result.size)
        assertEquals("c1", result[0].card.id)
    }

    @Test
    fun handlesCashbackRewardType() {
        val card = TestData.card(
            id = "c1",
            rewardRules = listOf(
                TestData.rule(id = "r1", cardId = "c1", multiplier = 0.02, rewardType = RewardType.CASHBACK)
            )
        )

        val result = engine.rankCards(listOf(card), SpendingCategory.DINING, 100.0)

        assertEquals(1, result.size)
        assertEquals(2.0, result[0].estimatedRewardValue)
    }

    @Test
    fun normalizesPointsRewardType() {
        val card = TestData.card(
            id = "c1",
            rewardRules = listOf(
                TestData.rule(id = "r1", cardId = "c1", multiplier = 3.0, rewardType = RewardType.POINTS)
            )
        )

        val result = engine.rankCards(listOf(card), SpendingCategory.DINING, 100.0)
        // 100 * 3.0 * (1.0 / 100.0) = 3.0
        assertEquals(3.0, result[0].estimatedRewardValue)
    }

    @Test
    fun normalizesMilesRewardType() {
        val card = TestData.card(
            id = "c1",
            rewardRules = listOf(
                TestData.rule(id = "r1", cardId = "c1", multiplier = 2.0, rewardType = RewardType.MILES)
            )
        )

        val result = engine.rankCards(listOf(card), SpendingCategory.DINING, 100.0)
        // 100 * 2.0 * (1.5 / 100.0) = 3.0
        assertEquals(3.0, result[0].estimatedRewardValue)
    }

    @Test
    fun respectsCapAmount() {
        val card = TestData.card(
            id = "c1",
            rewardRules = listOf(
                TestData.rule(id = "r1", cardId = "c1", multiplier = 0.05, rewardType = RewardType.CASHBACK, capAmount = 50.0)
            )
        )

        val result = engine.rankCards(listOf(card), SpendingCategory.DINING, 200.0)
        // min(200, 50) * 0.05 = 2.5
        assertEquals(2.5, result[0].estimatedRewardValue)
    }

    @Test
    fun returnsZeroForNoMatchingCategory() {
        val card = TestData.card(
            id = "c1",
            rewardRules = listOf(
                TestData.rule(id = "r1", cardId = "c1", category = SpendingCategory.TRAVEL)
            )
        )

        val result = engine.rankCards(listOf(card), SpendingCategory.DINING, 100.0)

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].estimatedRewardValue)
    }

    @Test
    fun returnsEmptyForNoEligibleCards() {
        val result = engine.rankCards(emptyList(), SpendingCategory.DINING, 100.0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun picksBestRuleWhenMultipleForSameCategory() {
        val card = TestData.card(
            id = "c1",
            rewardRules = listOf(
                TestData.rule(id = "r1", cardId = "c1", multiplier = 1.0),
                TestData.rule(id = "r2", cardId = "c1", multiplier = 5.0),
            )
        )

        val result = engine.rankCards(listOf(card), SpendingCategory.DINING, 100.0)

        assertEquals(5.0, result[0].rule?.multiplier)
    }
}
