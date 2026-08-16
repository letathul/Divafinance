package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

/**
 * What a ledger entry did to the balance with a person.
 *
 * The amount stored alongside is always positive; [sign] is what gives it direction, so a
 * balance is a plain signed sum and a partial repayment needs no special case. Positive
 * overall means they owe you.
 *
 * Persisted by name, so these four constants are frozen once shipped — renaming one is a
 * data migration, not a refactor.
 */
@Serializable
enum class LedgerEntryKind(val displayName: String, val sign: Int) {
    /** You gave them money, or paid for something on their behalf. */
    LENT("Lent", 1),

    /** They gave you money. */
    BORROWED("Borrowed", -1),

    /** They paid you back, in full or in part. */
    REPAID_TO_ME("Repaid to me", -1),

    /** You paid them back, in full or in part. */
    REPAID_BY_ME("Repaid by me", 1);
}
