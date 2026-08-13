package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.domain.engine.RewardRecommendationEngine
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.first

class GetBestCardForCategoryUseCase(
    private val cardRepository: CardRepository,
    private val rewardRepository: RewardRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        category: SpendingCategory,
        amount: Double,
    ): List<CardRecommendation> {
        val cards = cardRepository.getAll().first().map { card ->
            val rules = rewardRepository.getByCardId(card.id)
            card.copy(rewardRules = rules)
        }

        val pointsValue = settingsRepository.get(UserSettings.KEY_POINTS_VALUE)?.toDoubleOrNull() ?: 1.0
        val milesValue = settingsRepository.get(UserSettings.KEY_MILES_VALUE)?.toDoubleOrNull() ?: 1.5

        val engine = RewardRecommendationEngine(
            pointsValueCents = pointsValue,
            milesValueCents = milesValue,
        )

        return engine.rankCards(cards, category, amount)
    }
}
