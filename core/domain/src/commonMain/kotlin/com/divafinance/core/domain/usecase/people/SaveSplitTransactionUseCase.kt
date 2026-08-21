package com.divafinance.core.domain.usecase.people

import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.common.roundToCents
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.domain.usecase.transactions.AddTransactionUseCase
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.LedgerEntryKind
import kotlin.math.abs
import kotlin.time.Clock

/**
 * One other person's portion of a split bill.
 *
 * [personId] is null for someone typed in fresh; they are matched by name against existing
 * people before a new record is created, so "Sam" and " sam " never become two people with
 * separate balances.
 */
data class SplitShareInput(
    val personId: String?,
    val name: String,
    val amount: Double,
)

/**
 * Records a bill the user paid for other people.
 *
 * Writes one transaction at the full charged amount — so the card balance stays true —
 * plus one `LENT` ledger entry per other participant, all pointing at that transaction.
 * The transaction's `othersShare` is what keeps the spend out of the user's own totals.
 */
class SaveSplitTransactionUseCase(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val personRepository: PersonRepository,
    private val ledgerRepository: LedgerRepository,
) {
    /**
     * [transaction] must already carry the full amount and the matching `othersShare`.
     *
     * Rejects input where the shares do not reconcile. SQLite cannot add a CHECK
     * constraint to an existing table, so this is the only place the
     * `0 <= othersShare <= amount` invariant can be enforced — if it is not enforced here,
     * it is not enforced anywhere.
     */
    suspend operator fun invoke(transaction: Transaction, shares: List<SplitShareInput>) {
        require(transaction.othersShare >= 0.0) {
            "othersShare cannot be negative, was ${transaction.othersShare}"
        }
        require(transaction.othersShare <= transaction.amount) {
            "othersShare ${transaction.othersShare} exceeds amount ${transaction.amount}"
        }
        require(shares.none { it.amount < 0.0 }) { "a share cannot be negative" }

        val shareTotal = shares.sumOf { it.amount }.roundToCents()
        require(abs(shareTotal - transaction.othersShare.roundToCents()) < TOLERANCE) {
            "shares total $shareTotal but othersShare is ${transaction.othersShare}"
        }

        // The transaction lands first: the ledger entries reference it, and a failure here
        // must not leave debts pointing at a spend that was never recorded.
        addTransactionUseCase(transaction)

        val now = Clock.System.now()
        shares.forEach { share ->
            if (share.amount <= 0.0) return@forEach

            val person = resolvePerson(share, now)
            ledgerRepository.insert(
                LedgerEntry(
                    id = UuidGenerator.generate(),
                    personId = person.id,
                    amount = share.amount,
                    currency = transaction.currency,
                    kind = LedgerEntryKind.LENT,
                    note = transaction.merchantName,
                    date = transaction.date,
                    transactionId = transaction.id,
                    createdAt = now,
                ),
            )
        }
    }

    /** Existing person by id, else by name, else a new record. */
    private suspend fun resolvePerson(share: SplitShareInput, now: kotlin.time.Instant): Person {
        share.personId?.let { id ->
            personRepository.getById(id)?.let { return it }
        }
        personRepository.findByName(share.name)?.let { return it }

        val created = Person(
            id = UuidGenerator.generate(),
            name = share.name.trim(),
            createdAt = now,
            updatedAt = now,
        )
        personRepository.insert(created)
        return created
    }

    private companion object {
        /** Half a cent — the shares are rounded, so exact equality would be too strict. */
        const val TOLERANCE = 0.005
    }
}
