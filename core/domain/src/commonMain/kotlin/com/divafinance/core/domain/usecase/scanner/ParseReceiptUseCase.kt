package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.common.localePrefersDayFirst
import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.ReceiptLineItem
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.ReceiptStatus
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Turns OCR text into a stored [Receipt].
 *
 * The row is written here rather than by the caller because it is the durable artifact the
 * scan history and "resume a pending receipt" both read: it has to exist before the user starts
 * reviewing, so a process death mid-review loses nothing.
 *
 * Extraction itself lives in [ReceiptParser], which is pure and carries the test suite.
 */
class ParseReceiptUseCase(
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository,
    /**
     * Defaults to unavailable so every platform without an on-device model — which is all of
     * them but recent iOS — needs no wiring and behaves exactly as before.
     */
    private val extractor: ReceiptExtractor = UnavailableReceiptExtractor,
    /**
     * Injected rather than read inside [ReceiptParser] so extraction stays pure, and as a
     * lambda rather than a value so tests can pin it without the host's locale leaking in.
     */
    private val dayFirst: () -> Boolean = ::localePrefersDayFirst,
) {
    /**
     * [lines] carry per-line geometry when the engine could supply it; passing them lets the
     * parser rebuild the receipt's visual rows. Omitting them falls back to parsing [ocrText]
     * line by line, which is what every non-image caller does.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        imagePath: String,
        ocrText: String,
        lines: List<OcrLine> = emptyList(),
        additionalPagePaths: List<String> = emptyList(),
    ): Receipt {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val rules = if (lines.isEmpty()) {
            ReceiptParser.parse(ocrText, today, dayFirst())
        } else {
            ReceiptParser.parse(lines, today, dayFirst())
        }
        val parsed = rules.filledInBy(extractedOrNull(ocrText))

        val receiptId = Uuid.random().toString()
        val receipt = Receipt(
            id = receiptId,
            imagePath = imagePath,
            ocrText = ocrText,
            merchantName = parsed.merchantName,
            totalAmount = parsed.totalAmount,
            date = parsed.date,
            // PROCESSED means "linked to a transaction" and is set by ConfirmReceiptUseCase —
            // never here. A freshly parsed receipt is PENDING no matter how well it parsed,
            // which is what makes it show up as resumable in the scan history.
            status = if (ocrText.isBlank()) ReceiptStatus.FAILED else ReceiptStatus.PENDING,
            createdAt = Clock.System.now(),
            subtotalAmount = parsed.subtotal,
            taxAmount = parsed.tax,
            tipAmount = parsed.tip,
            currency = parsed.currency,
            pagePaths = additionalPagePaths,
            lineItems = parsed.items.mapIndexed { index, item ->
                ReceiptLineItem(
                    id = Uuid.random().toString(),
                    receiptId = receiptId,
                    position = index,
                    description = item.description,
                    quantity = item.quantity,
                    // The parser reads what a line was charged, which is the line total. A
                    // unit price is only knowable when a quantity was printed too.
                    unitPrice = item.quantity
                        ?.takeIf { it > 0.0 }
                        ?.let { quantity -> item.price?.div(quantity) },
                    totalPrice = item.price,
                )
            },
        )
        receiptRepository.insert(receipt)
        return receipt
    }

    /**
     * Runs the on-device model, or doesn't. Every step is a reason to skip it, and every
     * failure is swallowed: the rules-based parse has already produced a usable result and
     * losing it because a model call threw would be a strictly worse outcome.
     */
    private suspend fun extractedOrNull(ocrText: String): ExtractedReceipt? {
        if (ocrText.isBlank()) return null
        val enabled = runCatching {
            settingsRepository.get(UserSettings.KEY_SMART_RECEIPT_READING)
        }.getOrNull()?.toBooleanStrictOrNull() ?: SMART_READING_DEFAULT
        if (!enabled) return null
        val availability = runCatching { extractor.availability() }.getOrNull()
        if (availability != ExtractorAvailability.READY) return null
        return runCatching { extractor.extract(ocrText) }.getOrNull()
    }

    private companion object {
        /** On where a model exists: the setting is only reachable on those devices anyway. */
        const val SMART_READING_DEFAULT = true
    }
}

/**
 * The rules win every field they could read; the model only fills the gaps and supplies what
 * the rules never look for.
 *
 * This ordering is the whole safety argument for the feature. A regex that found `45.36` on the
 * total line found it because the receipt says so, whereas a model can produce a plausible
 * number for reasons it cannot explain — so a model may never overwrite an amount, a date or a
 * merchant that was actually read off the page. Where the rules found nothing, a plausible
 * guess the user can see and correct beats an empty field.
 */
internal fun ParsedReceipt.filledInBy(extracted: ExtractedReceipt?): ParsedReceipt {
    if (extracted == null) return this
    return copy(
        merchantName = merchantName ?: extracted.merchantName?.takeIf { it.isNotBlank() },
        totalAmount = totalAmount ?: extracted.totalAmount?.takeIf { it > 0.0 },
        date = date ?: extracted.date,
        subtotal = subtotal ?: extracted.subtotal,
        tax = tax ?: extracted.tax,
        tip = tip ?: extracted.tip,
        currency = currency ?: extracted.currency?.takeIf { it.isNotBlank() },
        // Items are all-or-nothing rather than merged: two partial lists interleaved would
        // double-count anything both found, and there is no key to match them on.
        items = items.ifEmpty { extracted.items.filter { it.description.isNotBlank() } },
    )
}
