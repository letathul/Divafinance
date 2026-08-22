package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.ReceiptLineItem
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReceiptRepository : ReceiptRepository {
    private val receipts = MutableStateFlow<List<Receipt>>(emptyList())

    /**
     * Held beside the receipts rather than on them, mirroring the real repository: the
     * list-returning reads there leave `lineItems` empty, so a test that reads items back
     * through [getLineItems] exercises the same shape production does.
     */
    private val itemsByReceipt = mutableMapOf<String, List<ReceiptLineItem>>()

    override fun getAll(): Flow<List<Receipt>> = receipts

    // The single-receipt reads attach line items and the list reads do not, matching
    // ReceiptRepositoryImpl — a fake that always attached them would hide the fact that a
    // caller reading from getAll() never sees an item.
    override suspend fun getById(id: String): Receipt? =
        receipts.value.find { it.id == id }?.withItems()

    override suspend fun getByTransactionId(transactionId: String): Receipt? =
        receipts.value.find { it.transactionId == transactionId }?.withItems()

    private fun Receipt.withItems(): Receipt =
        itemsByReceipt[id]?.let { copy(lineItems = it) } ?: this

    override suspend fun getByStatus(status: ReceiptStatus): List<Receipt> =
        receipts.value.filter { it.status == status }

    override suspend fun getLineItems(receiptId: String): List<ReceiptLineItem> =
        itemsByReceipt[receiptId].orEmpty()

    override suspend fun insert(receipt: Receipt) {
        receipts.value = receipts.value + receipt
        itemsByReceipt[receipt.id] = receipt.lineItems
    }

    override suspend fun update(receipt: Receipt) {
        receipts.value = receipts.value.map { if (it.id == receipt.id) receipt else it }
        itemsByReceipt[receipt.id] = receipt.lineItems
    }

    override suspend fun replaceLineItems(receiptId: String, items: List<ReceiptLineItem>) {
        itemsByReceipt[receiptId] = items
    }

    override suspend fun delete(id: String) {
        receipts.value = receipts.value.filter { it.id != id }
        itemsByReceipt.remove(id)
    }

    fun getReceipts(): List<Receipt> = receipts.value

    /** Seeds without needing a suspend context, mirroring `FakeCardRepository.setCards`. */
    fun setReceipts(list: List<Receipt>) {
        receipts.value = list
    }

    /**
     * Seeds a receipt's items separately, because that is how they exist in the real
     * repository: [setReceipts] leaves `lineItems` empty exactly as `getAll()` does.
     */
    fun setLineItems(receiptId: String, items: List<ReceiptLineItem>) {
        itemsByReceipt[receiptId] = items
    }

    /** Reads items back without a suspend context, for assertions. */
    fun lineItemsOf(receiptId: String): List<ReceiptLineItem> = itemsByReceipt[receiptId].orEmpty()
}
