package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.ReceiptStatus

/**
 * Turns a reviewed [Receipt] into a real [Transaction] and links the two.
 *
 * Both writes live here so the link can never be half-formed by a caller that remembers one
 * side and forgets the other. `transaction.receiptId` must already point at [receipt] — the id
 * is minted by the caller before the insert, because [AddTransactionUseCase] returns Unit and
 * so cannot hand one back.
 *
 * Ordering is deliberate: the transaction lands first. If that throws, the receipt stays
 * PENDING and is still resumable from the scan history, so nothing is lost. The reverse order
 * could mark a receipt PROCESSED against a transaction that never landed.
 */
class ConfirmReceiptUseCase(
    private val receiptRepository: ReceiptRepository,
    private val addTransaction: AddTransactionUseCase,
) {
    suspend operator fun invoke(receipt: Receipt, transaction: Transaction) {
        addTransaction(transaction)
        receiptRepository.update(
            receipt.copy(
                transactionId = transaction.id,
                merchantName = transaction.merchantName ?: receipt.merchantName,
                totalAmount = transaction.amount,
                date = transaction.date,
                status = ReceiptStatus.PROCESSED,
            ),
        )
    }
}
