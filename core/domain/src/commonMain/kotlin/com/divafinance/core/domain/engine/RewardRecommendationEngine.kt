package com.divafinance.core.domain.engine

import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory

data class CardRecommendation(
    val card: CreditCard,
    val rule: CardRewardRule?,
    val estimatedRewardValue: Double,
    val remainingCap: Double?,
    val availableCredit: Double,
)

class RewardRecommendationEngine(
    private val pointsValueCents: Double = 1.0,
    private val milesValueCents: Double = 1.5,
) {

    fun rankCards(
        cards: List<CreditCard>,
        category: SpendingCategory,
        amount: Double,
    ): List<CardRecommendation> {
        return cards
            .filter { it.isActive && it.availableCredit >= amount }
            .map { card ->
                val rule = card.rewardRules
                    .filter { it.isActive && it.category == category }
                    .maxByOrNull { it.multiplier }

                val estimatedValue = if (rule != null) {
                    val effectiveAmount = rule.capAmount?.let { cap ->
                        amount.coerceAtMost(cap)
                    } ?: amount

                    val rawReward = effectiveAmount * rule.multiplier

                    when (rule.rewardType) {
                        RewardType.CASHBACK -> rawReward
                        RewardType.POINTS -> rawReward * (pointsValueCents / 100.0)
                        RewardType.MILES -> rawReward * (milesValueCents / 100.0)
                    }
                } else {
                    0.0
                }

                CardRecommendation(
                    card = card,
                    rule = rule,
                    estimatedRewardValue = estimatedValue,
                    remainingCap = rule?.capAmount,
                    availableCredit = card.availableCredit,
                )
            }
            .sortedByDescending { it.estimatedRewardValue }
    }
}
