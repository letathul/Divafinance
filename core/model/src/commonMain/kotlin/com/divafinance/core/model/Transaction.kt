package com.divafinance.core.model

import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Instant
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
    /**
     * The part of [amount] that other people owe back, from a split bill.
     *
     * [amount] stays the full figure the card was charged, so card balances and statement
     * reconciliation stay correct; spending aggregates subtract this so only the user's
     * own share counts as consumption.
     *
     * Set once when the split is created and **never reduced by repayments** — being paid
     * back does not retroactively turn a shared bill into your own spending, and treating
     * it that way would silently rewrite historical reports.
     */
    val othersShare: Double = 0.0,
)
