package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.RewardType

class CalculateRewardValueUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(rule: CardRewardRule, amount: Double): Double {
        val effectiveAmount = rule.capAmount?.let { amount.coerceAtMost(it) } ?: amount
        val rawReward = effectiveAmount * rule.multiplier

        return when (rule.rewardType) {
            RewardType.CASHBACK -> rawReward
            RewardType.POINTS -> {
                val pointsValue = settingsRepository.get(UserSettings.KEY_POINTS_VALUE)?.toDoubleOrNull() ?: 1.0
                rawReward * (pointsValue / 100.0)
            }
            RewardType.MILES -> {
                val milesValue = settingsRepository.get(UserSettings.KEY_MILES_VALUE)?.toDoubleOrNull() ?: 1.5
                rawReward * (milesValue / 100.0)
            }
        }
    }
}
