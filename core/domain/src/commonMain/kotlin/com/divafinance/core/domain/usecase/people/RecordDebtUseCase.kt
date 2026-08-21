package com.divafinance.core.domain.usecase.people

import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import com.divafinance.core.model.enums.LedgerEntryKind
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Records money moving between the user and a person, outside any bill.
 *
 * Covers plain cash — "I gave Sam £20", "Alex covered my ticket" — which is the other half
 * of the feature. Splits go through [SaveSplitTransactionUseCase] because they also create
 * a transaction; this deliberately creates none, since handing over cash you expect back
 * is not spending.
 */
class RecordDebtUseCase(
    private val personRepository: PersonRepository,
    private val ledgerRepository: LedgerRepository,
) {
    /**
     * [name] is matched against existing people before a new record is created, so the
     * same person never ends up with their balance split across duplicates.
     *
     * Returns the person the entry was filed against.
     */
    suspend operator fun invoke(
        name: String,
        amount: Double,
        kind: LedgerEntryKind,
        personId: String? = null,
        note: String? = null,
        currency: String = "USD",
        date: LocalDate = today(),
    ): Person {
        require(amount > 0.0) { "amount must be greater than zero, was $amount" }
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() || personId != null) { "a person is required" }

        val now = Clock.System.now()
        val person = resolvePerson(personId, trimmed, now)

        ledgerRepository.insert(
            LedgerEntry(
                id = UuidGenerator.generate(),
                personId = person.id,
                amount = amount,
                currency = currency,
                kind = kind,
                note = note?.takeIf { it.isNotBlank() },
                date = date,
                transactionId = null,
                createdAt = now,
            ),
        )
        return person
    }

    private suspend fun resolvePerson(
        personId: String?,
        name: String,
        now: kotlin.time.Instant,
    ): Person {
        personId?.let { id -> personRepository.getById(id)?.let { return it } }
        personRepository.findByName(name)?.let { return it }

        val created = Person(
            id = UuidGenerator.generate(),
            name = name,
            createdAt = now,
            updatedAt = now,
        )
        personRepository.insert(created)
        return created
    }
}

internal fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
