package com.divafinance.core.model

import com.divafinance.core.model.enums.ReceiptStatus
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Receipt(
    val id: String,
    val transactionId: String? = null,
    val imagePath: String? = null,
    val ocrText: String? = null,
    val merchantName: String? = null,
    val totalAmount: Double? = null,
    val date: LocalDate? = null,
    val status: ReceiptStatus = ReceiptStatus.PENDING,
    val createdAt: Instant,
    /**
     * The other amounts a receipt prints. Null means "not recorded" rather than zero — a
     * receipt scanned before these were extracted genuinely has no value for them, and a 0.0
     * would read as "no tax was charged".
     */
    val subtotalAmount: Double? = null,
    val taxAmount: Double? = null,
    val tipAmount: Double? = null,
    /** As printed. Falls back to the user's base currency when the receipt doesn't say. */
    val currency: String? = null,
    /**
     * Pages 2..n of a multi-page scan. Page 1 stays in [imagePath], so single-page receipts and
     * every existing reader are untouched.
     */
    val pagePaths: List<String> = emptyList(),
    /**
     * Not a column — rebuilt from `ReceiptLineItem` on read and written back wholesale on save.
     * Carried on the receipt because nothing ever wants one without the other.
     */
    val lineItems: List<ReceiptLineItem> = emptyList(),
)
