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
)
