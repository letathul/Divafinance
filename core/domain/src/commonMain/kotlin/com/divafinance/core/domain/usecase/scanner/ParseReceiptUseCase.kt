package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.enums.ReceiptStatus
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ParseReceiptUseCase(
    private val receiptRepository: ReceiptRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(imagePath: String, ocrText: String): Receipt {
        val merchantName = extractMerchant(ocrText)
        val totalAmount = extractTotal(ocrText)

        val receipt = Receipt(
            id = Uuid.random().toString(),
            imagePath = imagePath,
            ocrText = ocrText,
            merchantName = merchantName,
            totalAmount = totalAmount,
            status = if (totalAmount != null) ReceiptStatus.PROCESSED else ReceiptStatus.PENDING,
            createdAt = Clock.System.now(),
        )
        receiptRepository.insert(receipt)
        return receipt
    }

    private fun extractMerchant(text: String): String? {
        return text.lines().firstOrNull { it.isNotBlank() }?.trim()
    }

    private fun extractTotal(text: String): Double? {
        val totalPattern = Regex("""(?i)total[:\s]*\$?([\d,]+\.?\d*)""")
        return totalPattern.find(text)?.groupValues?.getOrNull(1)
            ?.replace(",", "")?.toDoubleOrNull()
    }
}
