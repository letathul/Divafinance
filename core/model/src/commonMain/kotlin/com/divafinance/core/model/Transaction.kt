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
    /**
     * Free-text labels, orthogonal to [category]. A transaction has exactly one category
     * because reports have to bucket it exactly once; tags are the place for the
     * cross-cutting facts a single bucket cannot carry ("holiday", "reimbursable").
     *
     * Stored newline-delimited in one column, so a tag may not contain a newline.
     */
    val tags: List<String> = emptyList(),
    /**
     * The [CustomCategory] this was filed under, when the user picked one of their own.
     *
     * Null means [category] is shown as itself. When set, [category] still holds that
     * custom category's parent — so every engine keeps reading one enum and only the UI
     * resolves the custom name, icon and colour.
     */
    val customCategoryId: String? = null,
)
