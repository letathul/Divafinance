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
 * Records a split bill, whoever paid for it.
 *
 * **The user paid** (`payer == null`): one transaction at the full charged amount — so the
 * card balance stays true — plus one `LENT` entry per other participant. The
 * transaction's `othersShare` keeps the part they owe back out of the user's own totals.
 *
 * **Someone else paid**: the user consumed their share but was never charged, so the
 * transaction carries no card and the ledger runs the other way — a single `BORROWED`
 * entry against the payer for the user's own share. The other participants' portions are
 * between them and the payer; recording them here would invent debts the user is not party
 * to. `othersShare` means the same thing in both cases: the part of the bill that is not
 * the user's consumption.
 */
class SaveSplitTransactionUseCase(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val personRepository: PersonRepository,
    private val ledgerRepository: LedgerRepository,
) {
    /**
     * [transaction] must already carry the full amount and the matching `othersShare`.
     *
     * [payer] is null when the user paid — the ordinary case. When it is set, [shares] is
     * ignored: what the other participants owe is not the user's ledger to record.
     *
     * Rejects input where the shares do not reconcile. SQLite cannot add a CHECK
     * constraint to an existing table, so this is the only place the
     * `0 <= othersShare <= amount` invariant can be enforced — if it is not enforced here,
     * it is not enforced anywhere.
     */
    suspend operator fun invoke(
        transaction: Transaction,
        shares: List<SplitShareInput>,
        payer: SplitShareInput? = null,
    ) {
        require(transaction.othersShare >= 0.0) {
            "othersShare cannot be negative, was ${transaction.othersShare}"
        }
        require(transaction.othersShare <= transaction.amount) {
            "othersShare ${transaction.othersShare} exceeds amount ${transaction.amount}"
        }
        require(shares.none { it.amount < 0.0 }) { "a share cannot be negative" }

        if (payer != null) {
            // A bill the user did not pay is not a charge on any of their cards, and
            // AddTransactionUseCase would raise a card balance for money that never left it.
            require(transaction.cardId == null) {
                "a transaction someone else paid for cannot be charged to a card"
            }
            savePaidByOther(transaction, payer)
            return
        }

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

    /**
     * The someone-else-paid branch: the user owes the payer exactly what the user consumed,
     * which is the whole bill less the part that was never theirs.
     */
    private suspend fun savePaidByOther(transaction: Transaction, payer: SplitShareInput) {
        addTransactionUseCase(transaction)

        val ownShare = (transaction.amount - transaction.othersShare).roundToCents()
        // A bill someone else paid where the user consumed nothing is not the user's
        // transaction at all, but it is recorded rather than refused — a zero-value debt
        // is simply not worth writing.
        if (ownShare <= 0.0) return

        val now = Clock.System.now()
        val person = resolvePerson(payer, now)
        ledgerRepository.insert(
            LedgerEntry(
                id = UuidGenerator.generate(),
                personId = person.id,
                amount = ownShare,
                currency = transaction.currency,
                kind = LedgerEntryKind.BORROWED,
                note = transaction.merchantName,
                date = transaction.date,
                transactionId = transaction.id,
                createdAt = now,
            ),
        )
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
