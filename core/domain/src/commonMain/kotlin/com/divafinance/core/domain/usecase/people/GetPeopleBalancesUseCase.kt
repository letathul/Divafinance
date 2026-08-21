package com.divafinance.core.domain.usecase.people

import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Everyone, with what they owe or are owed.
 *
 * Combines both flows so the list re-emits when either a person or an entry changes —
 * settling a debt updates the row without anything having to invalidate it by hand.
 */
class GetPeopleBalancesUseCase(
    private val personRepository: PersonRepository,
    private val ledgerRepository: LedgerRepository,
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<PersonBalance>> {
        val people = if (includeArchived) personRepository.getAll() else personRepository.getActive()

        return combine(people, ledgerRepository.getAll()) { persons, entries ->
            val byPerson = entries.groupBy { it.personId }
            persons.map { person ->
                val theirs = byPerson[person.id].orEmpty()
                PersonBalance(
                    person = person,
                    balance = theirs.balance(),
                    entryCount = theirs.size,
                )
            }
        }
    }
}
