package com.divafinance.core.domain.usecase.people

import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.enums.LedgerEntryKind
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

/**
 * Records a repayment against a person's balance.
 *
 * The direction is derived from what is currently owed rather than passed in, so the
 * caller cannot record a repayment that moves the balance the wrong way. Partial amounts
 * are fine — the balance is a signed sum, so nothing needs to track how much is left.
 */
class SettleUpUseCase(
    private val ledgerRepository: LedgerRepository,
) {
    /**
     * Returns false when there is nothing to settle. Repaying against a zero balance would
     * create a debt in the opposite direction, which is never what "settle up" means.
     */
    suspend operator fun invoke(
        personId: String,
        amount: Double,
        currency: String = "USD",
        note: String? = null,
        date: LocalDate = today(),
    ): Boolean {
        require(amount > 0.0) { "amount must be greater than zero, was $amount" }

        val balance = ledgerRepository.getByPersonId(personId).balance()
        if (abs(balance) < PersonBalance.SETTLED_EPSILON) return false

        ledgerRepository.insert(
            LedgerEntry(
                id = UuidGenerator.generate(),
                personId = personId,
                amount = amount,
                currency = currency,
                // They owed you, so their paying reduces it; otherwise you are paying them.
                kind = if (balance > 0.0) LedgerEntryKind.REPAID_TO_ME else LedgerEntryKind.REPAID_BY_ME,
                note = note?.takeIf { it.isNotBlank() },
                date = date,
                transactionId = null,
                createdAt = Clock.System.now(),
            ),
        )
        return true
    }

    /** What it would take to clear the balance completely. Zero when already settled. */
    suspend fun outstandingFor(personId: String): Double =
        abs(ledgerRepository.getByPersonId(personId).balance())
            .takeIf { it >= PersonBalance.SETTLED_EPSILON } ?: 0.0
}
