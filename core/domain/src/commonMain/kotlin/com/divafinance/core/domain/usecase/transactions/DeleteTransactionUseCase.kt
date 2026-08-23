package com.divafinance.core.domain.usecase.transactions

import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.ReceiptFileStore
import com.divafinance.core.data.repository.ReceiptRepository
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
 * dinner leaves everyone still owing you for it. A scanned receipt owns a row *and* image
 * files on disk, and both go too.
 */
class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val ledgerRepository: LedgerRepository,
    private val receiptRepository: ReceiptRepository,
    private val receiptFileStore: ReceiptFileStore,
) {
    suspend operator fun invoke(transactionId: String) {
        val transaction = transactionRepository.getById(transactionId) ?: return

        // Before the delete: `Receipt.transaction_id` is how the receipt is found, so
        // removing the transaction first would strand the row and its files permanently.
        deleteReceiptFor(transactionId)

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

    /**
     * Page 1 lives in `imagePath` and pages 2..n in `pagePaths`, so a multi-page scan
     * leaves files behind unless both are walked. A path that is already gone is not an
     * error — the row is what we are authoritative about, not the disk.
     */
    private suspend fun deleteReceiptFor(transactionId: String) {
        val receipt = receiptRepository.getByTransactionId(transactionId) ?: return
        (listOfNotNull(receipt.imagePath) + receipt.pagePaths).forEach(receiptFileStore::delete)
        receiptRepository.delete(receipt.id)
    }
}
