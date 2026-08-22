package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.ReceiptLineItem
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Line items are read alongside their receipt everywhere a single receipt is fetched, but not
 * for [getAll] or [getByStatus]: those feed the history list, which shows a merchant and a
 * total, and joining a second table per row for data nothing renders would be a query per
 * receipt for nothing.
 */
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
        val receipt = db.receiptQueries.selectById(id).executeAsOneOrNull()?.toDomain()
            ?: return null
        return receipt.copy(lineItems = getLineItems(id))
    }

    override suspend fun getByTransactionId(transactionId: String): Receipt? {
        val receipt = db.receiptQueries.selectByTransactionId(transactionId)
            .executeAsOneOrNull()
            ?.toDomain()
            ?: return null
        return receipt.copy(lineItems = getLineItems(receipt.id))
    }

    override suspend fun getByStatus(status: ReceiptStatus): List<Receipt> {
        return db.receiptQueries.selectByStatus(status.name)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getLineItems(receiptId: String): List<ReceiptLineItem> {
        return db.receiptLineItemQueries.selectByReceipt(receiptId)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun insert(receipt: Receipt) {
        db.transaction {
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
                subtotal_amount = receipt.subtotalAmount,
                tax_amount = receipt.taxAmount,
                tip_amount = receipt.tipAmount,
                currency = receipt.currency,
                page_paths = receipt.pagePaths.encodePagePaths(),
            )
            writeLineItems(receipt.id, receipt.lineItems)
        }
    }

    override suspend fun update(receipt: Receipt) {
        db.transaction {
            db.receiptQueries.update(
                transaction_id = receipt.transactionId,
                image_path = receipt.imagePath,
                ocr_text = receipt.ocrText,
                merchant_name = receipt.merchantName,
                total_amount = receipt.totalAmount,
                date = receipt.date?.toString(),
                status = receipt.status.name,
                subtotal_amount = receipt.subtotalAmount,
                tax_amount = receipt.taxAmount,
                tip_amount = receipt.tipAmount,
                currency = receipt.currency,
                page_paths = receipt.pagePaths.encodePagePaths(),
                id = receipt.id,
            )
            writeLineItems(receipt.id, receipt.lineItems)
        }
    }

    override suspend fun replaceLineItems(receiptId: String, items: List<ReceiptLineItem>) {
        db.transaction { writeLineItems(receiptId, items) }
    }

    override suspend fun delete(id: String) {
        db.transaction {
            db.receiptLineItemQueries.deleteByReceipt(id)
            db.receiptQueries.delete(id)
        }
    }

    /**
     * Delete-then-insert rather than a diff: the item list is small, always rewritten as a
     * whole when a review is saved, and a diff would have to invent stable identities for rows
     * the user can freely reorder.
     */
    private fun writeLineItems(receiptId: String, items: List<ReceiptLineItem>) {
        db.receiptLineItemQueries.deleteByReceipt(receiptId)
        items.forEachIndexed { index, item ->
            db.receiptLineItemQueries.insert(
                id = item.id,
                receipt_id = receiptId,
                position = index.toLong(),
                description = item.description,
                quantity = item.quantity,
                unit_price = item.unitPrice,
                total_price = item.totalPrice,
            )
        }
    }
}

/**
 * Newline-separated, because a file path may contain almost anything except a newline and this
 * avoids a JSON dependency in the mapper for a list that is nearly always empty.
 */
private fun List<String>.encodePagePaths(): String? =
    if (isEmpty()) null else joinToString("\n")

private fun String?.decodePagePaths(): List<String> =
    this?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

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
    subtotalAmount = subtotal_amount,
    taxAmount = tax_amount,
    tipAmount = tip_amount,
    currency = currency,
    pagePaths = page_paths.decodePagePaths(),
)

private fun com.divafinance.core.database.ReceiptLineItem.toDomain() = ReceiptLineItem(
    id = id,
    receiptId = receipt_id,
    position = position.toInt(),
    description = description,
    quantity = quantity,
    unitPrice = unit_price,
    totalPrice = total_price,
)
