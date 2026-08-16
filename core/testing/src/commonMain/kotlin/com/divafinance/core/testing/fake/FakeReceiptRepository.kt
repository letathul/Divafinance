package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReceiptRepository : ReceiptRepository {
    private val receipts = MutableStateFlow<List<Receipt>>(emptyList())

    override fun getAll(): Flow<List<Receipt>> = receipts

    override suspend fun getById(id: String): Receipt? =
        receipts.value.find { it.id == id }

    override suspend fun getByTransactionId(transactionId: String): Receipt? =
        receipts.value.find { it.transactionId == transactionId }

    override suspend fun getByStatus(status: ReceiptStatus): List<Receipt> =
        receipts.value.filter { it.status == status }

    override suspend fun insert(receipt: Receipt) {
        receipts.value = receipts.value + receipt
    }

    override suspend fun update(receipt: Receipt) {
        receipts.value = receipts.value.map { if (it.id == receipt.id) receipt else it }
    }

    override suspend fun delete(id: String) {
        receipts.value = receipts.value.filter { it.id != id }
    }

    fun getReceipts(): List<Receipt> = receipts.value
}
