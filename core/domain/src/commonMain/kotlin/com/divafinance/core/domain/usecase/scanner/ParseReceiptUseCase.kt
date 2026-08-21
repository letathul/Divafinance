package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.model.Receipt
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
    private val receiptRepository: ReceiptRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(imagePath: String, ocrText: String): Receipt {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val parsed = ReceiptParser.parse(ocrText, today)

        val receipt = Receipt(
            id = Uuid.random().toString(),
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
        )
        receiptRepository.insert(receipt)
        return receipt
    }
}
