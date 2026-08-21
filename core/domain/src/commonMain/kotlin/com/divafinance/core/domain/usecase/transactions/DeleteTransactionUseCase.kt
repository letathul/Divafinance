package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.enums.TransactionType

/**
 * The exact inverse of [AddTransactionUseCase].
 *
 * Adding a DEBIT against a card raises that card's balance, so deleting the row without
 * lowering it again leaves the card permanently overstated. Anything that removes a
 * transaction — undo, an edit that reassigns the card, a manual delete — must go through
 * here rather than calling [TransactionRepository.delete] directly.
 *
 * A split bill also owns ledger entries, which have to go with it; otherwise deleting the
 * dinner leaves everyone still owing you for it.
 */
class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val ledgerRepository: LedgerRepository,
) {
    suspend operator fun invoke(transactionId: String) {
        val transaction = transactionRepository.getById(transactionId) ?: return
        transactionRepository.delete(transactionId)

        // Deliberately before the card work: that block returns early when the card has
        // since been removed, and the debts must not survive that path.
        ledgerRepository.deleteByTransactionId(transactionId)

        val cardId = transaction.cardId
        if (transaction.type == TransactionType.DEBIT && cardId != null) {
            val card = cardRepository.getById(cardId) ?: return
            cardRepository.updateBalance(
                id = card.id,
                balance = card.currentBalance - transaction.amount,
            )
        }
    }
}
