package com.divafinance.core.model

import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val cardId: String? = null,
    val amount: Double,
    val currency: String = "USD",
    val category: SpendingCategory,
    val subcategory: String? = null,
    val merchantName: String? = null,
    val note: String? = null,
    val date: LocalDate,
    val type: TransactionType,
    val location: LocationTag? = null,
    val receiptId: String? = null,
    val isRecurring: Boolean = false,
    val createdAt: Instant,
)
