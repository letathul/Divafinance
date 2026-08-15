package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class ReceiptRepositoryImpl(
    private val db: DivaFinanceDb
) : ReceiptRepository {

    override fun getAll(): Flow<List<Receipt>> {
        return db.receiptQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Receipt? {
        return db.receiptQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getByTransactionId(transactionId: String): Receipt? {
        return db.receiptQueries.selectByTransactionId(transactionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getByStatus(status: ReceiptStatus): List<Receipt> {
        return db.receiptQueries.selectByStatus(status.name)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun insert(receipt: Receipt) {
        db.receiptQueries.insert(
            id = receipt.id,
            transaction_id = receipt.transactionId,
            image_path = receipt.imagePath,
            ocr_text = receipt.ocrText,
            merchant_name = receipt.merchantName,
            total_amount = receipt.totalAmount,
            date = receipt.date?.toString(),
            status = receipt.status.name,
            created_at = receipt.createdAt.toString(),
        )
    }

    override suspend fun update(receipt: Receipt) {
        db.receiptQueries.update(
            transaction_id = receipt.transactionId,
            image_path = receipt.imagePath,
            ocr_text = receipt.ocrText,
            merchant_name = receipt.merchantName,
            total_amount = receipt.totalAmount,
            date = receipt.date?.toString(),
            status = receipt.status.name,
            id = receipt.id,
        )
    }

    override suspend fun delete(id: String) {
        db.receiptQueries.delete(id)
    }
}

private fun com.divafinance.core.database.Receipt.toDomain() = Receipt(
    id = id,
    transactionId = transaction_id,
    imagePath = image_path,
    ocrText = ocr_text,
    merchantName = merchant_name,
    totalAmount = total_amount,
    date = date?.let { LocalDate.parse(it) },
    status = runCatching { ReceiptStatus.valueOf(status) }.getOrDefault(ReceiptStatus.PENDING),
    createdAt = Instant.parse(created_at),
)
