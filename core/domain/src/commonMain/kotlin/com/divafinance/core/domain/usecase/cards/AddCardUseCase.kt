package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.model.CreditCard

class AddCardUseCase(
    private val cardRepository: CardRepository,
    private val rewardRepository: RewardRepository,
) {
    suspend operator fun invoke(card: CreditCard) {
        cardRepository.insert(card)
        card.rewardRules.forEach { rule ->
            rewardRepository.insert(rule)
        }
    }
}
