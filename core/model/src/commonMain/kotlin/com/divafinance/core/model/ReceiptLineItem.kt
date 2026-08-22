package com.divafinance.core.model

import kotlinx.serialization.Serializable

/**
 * One purchased item as printed on a receipt.
 *
 * Every figure is nullable because receipts are inconsistent about what they print: plenty show
 * only a description and a price, and a quantity invented to fill the gap would be a number the
 * user never saw. [description] is the one thing always present, and the one thing that makes
 * the row worth keeping at all.
 */
@Serializable
data class ReceiptLineItem(
    val id: String,
    val receiptId: String,
    /** Printed order, zero-based. Rows are rewritten wholesale, so this is stored not inferred. */
    val position: Int,
    val description: String,
    val quantity: Double? = null,
    val unitPrice: Double? = null,
    val totalPrice: Double? = null,
)
