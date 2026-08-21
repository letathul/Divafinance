package com.divafinance.core.domain.usecase.people

import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import kotlin.math.abs

/** A person alongside what they currently owe, or are owed. */
data class PersonBalance(
    val person: Person,
    /** Positive means they owe you; negative means you owe them. */
    val balance: Double,
    val entryCount: Int,
) {
    /**
     * Settled means "within half a minor unit of zero", not `== 0.0`.
     *
     * Amounts are stored as doubles, so a sequence that should cancel exactly can leave a
     * residue far too small to display but large enough to fail an equality check — which
     * would leave a debt looking permanently unsettled.
     */
    val isSettled: Boolean get() = abs(balance) < SETTLED_EPSILON

    val theyOweMe: Boolean get() = !isSettled && balance > 0.0
    val iOweThem: Boolean get() = !isSettled && balance < 0.0

    companion object {
        /** Half a cent — below anything that can be displayed at two decimal places. */
        const val SETTLED_EPSILON = 0.005
    }
}

/** Positive means they owe you. */
fun List<LedgerEntry>.balance(): Double = sumOf { it.signedAmount }
