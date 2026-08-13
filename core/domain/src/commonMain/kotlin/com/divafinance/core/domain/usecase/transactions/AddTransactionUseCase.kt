package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.TransactionType

class AddTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(transaction: Transaction) {
        transactionRepository.insert(transaction)

        val cardId = transaction.cardId
        if (transaction.type == TransactionType.DEBIT && cardId != null) {
            val card = cardRepository.getById(cardId) ?: return
            cardRepository.updateBalance(
                id = card.id,
                balance = card.currentBalance + transaction.amount,
            )
        }
    }
}
