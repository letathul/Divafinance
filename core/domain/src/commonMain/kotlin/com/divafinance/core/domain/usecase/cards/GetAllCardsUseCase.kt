package com.divafinance.core.domain.usecase.cards

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.model.CreditCard
import kotlinx.coroutines.flow.Flow

class GetAllCardsUseCase(
    private val cardRepository: CardRepository
) {
    operator fun invoke(): Flow<List<CreditCard>> = cardRepository.getAll()
}
