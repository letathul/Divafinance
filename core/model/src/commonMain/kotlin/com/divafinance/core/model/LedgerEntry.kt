package com.divafinance.core.model

import com.divafinance.core.model.enums.LedgerEntryKind
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One movement of money between the user and a [Person].
 *
 * [amount] is always positive; direction comes from [kind].
 */
@Serializable
data class LedgerEntry(
    val id: String,
    val personId: String,
    val amount: Double,
    val currency: String = "USD",
    val kind: LedgerEntryKind,
    val note: String? = null,
    val date: LocalDate,
    /**
     * Set when this came from a real spend. Also what groups the entries created by one
     * split bill, which is why there is no separate group id.
     */
    val transactionId: String? = null,
    val createdAt: Instant,
) {
    /** Contribution to the balance with this person. Positive means they owe you. */
    val signedAmount: Double get() = amount * kind.sign
}
