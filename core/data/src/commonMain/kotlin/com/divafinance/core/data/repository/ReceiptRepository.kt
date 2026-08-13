package com.divafinance.core.data.repository

import com.divafinance.core.model.Receipt
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun getAll(): Flow<List<Receipt>>
    suspend fun getById(id: String): Receipt?
    suspend fun getByTransactionId(transactionId: String): Receipt?
    suspend fun getByStatus(status: ReceiptStatus): List<Receipt>
    suspend fun insert(receipt: Receipt)
    suspend fun update(receipt: Receipt)
    suspend fun delete(id: String)
}
