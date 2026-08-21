package com.divafinance.core.domain.usecase.people

import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** One person, their balance, and everything that made it up. */
data class PersonDetail(
    val person: Person,
    val balance: PersonBalance,
    val entries: List<LedgerEntry>,
)

/**
 * Reactive so a settle-up updates the screen it was performed on, without the caller
 * having to reload anything. Emits null when the person no longer exists.
 */
class GetPersonDetailUseCase(
    private val personRepository: PersonRepository,
    private val ledgerRepository: LedgerRepository,
) {
    operator fun invoke(personId: String): Flow<PersonDetail?> =
        combine(personRepository.getAll(), ledgerRepository.getAll()) { people, entries ->
            val person = people.find { it.id == personId } ?: return@combine null
            val theirs = entries
                .filter { it.personId == personId }
                .sortedWith(compareByDescending<LedgerEntry> { it.date }.thenByDescending { it.createdAt })

            PersonDetail(
                person = person,
                balance = PersonBalance(person, theirs.balance(), theirs.size),
                entries = theirs,
            )
        }
}
