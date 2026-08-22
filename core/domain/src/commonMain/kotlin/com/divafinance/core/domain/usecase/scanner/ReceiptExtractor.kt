package com.divafinance.core.domain.usecase.scanner

import kotlinx.datetime.LocalDate

/** Whether an on-device model can be asked to read a receipt right now. */
enum class ExtractorAvailability {
    READY,

    /** The system is still fetching the model. Try again later; do not offer it as broken. */
    DOWNLOADING,

    /** No model on this device, or the user turned the feature off at the OS level. */
    UNSUPPORTED,
}

/**
 * What a model recovered from a block of OCR text. Deliberately the same shape as
 * [ParsedReceipt] so the two can be merged field by field.
 */
data class ExtractedReceipt(
    val merchantName: String? = null,
    val totalAmount: Double? = null,
    val date: LocalDate? = null,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val tip: Double? = null,
    val currency: String? = null,
    val items: List<ParsedLineItem> = emptyList(),
)

/**
 * An optional on-device language model that reads what [ReceiptParser]'s rules cannot —
 * item lists in particular, which no reasonable regex recovers from a messy receipt.
 *
 * An interface rather than an `expect class` for the same reason `LocationSource` is one in
 * `:core:common`: an expect class cannot be subclassed, so a test could not fake it.
 *
 * The contract is that this is always optional. [extract] returns null on any failure and
 * callers must already be correct without it — no field here is load-bearing.
 */
interface ReceiptExtractor {
    suspend fun availability(): ExtractorAvailability

    /** Null when the model is unavailable, declined, or returned something unusable. */
    suspend fun extract(ocrText: String): ExtractedReceipt?
}

/** The default everywhere no on-device model exists: Android, desktop, and older iOS. */
object UnavailableReceiptExtractor : ReceiptExtractor {
    override suspend fun availability(): ExtractorAvailability = ExtractorAvailability.UNSUPPORTED
    override suspend fun extract(ocrText: String): ExtractedReceipt? = null
}
