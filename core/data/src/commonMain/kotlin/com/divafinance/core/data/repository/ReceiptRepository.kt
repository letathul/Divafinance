package com.divafinance.core.data.repository

import com.divafinance.core.model.Receipt
import com.divafinance.core.model.ReceiptLineItem
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.flow.Flow

/**
 * [insert] and [update] write `Receipt.lineItems` along with the receipt, so a caller that has
 * both never needs [replaceLineItems]. The list-returning reads deliberately leave `lineItems`
 * empty — the history list shows a merchant and a total, and paying for a second query per row
 * to fill a field nothing renders would be waste.
 */
interface ReceiptRepository {
    fun getAll(): Flow<List<Receipt>>
    suspend fun getById(id: String): Receipt?
    suspend fun getByTransactionId(transactionId: String): Receipt?
    suspend fun getByStatus(status: ReceiptStatus): List<Receipt>
    suspend fun getLineItems(receiptId: String): List<ReceiptLineItem>
    suspend fun insert(receipt: Receipt)
    suspend fun update(receipt: Receipt)
    suspend fun replaceLineItems(receiptId: String, items: List<ReceiptLineItem>)
    suspend fun delete(id: String)
}
