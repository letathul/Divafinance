package com.divafinance.core.data.repository

import com.divafinance.core.model.Person
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun getAll(): Flow<List<Person>>
    fun getActive(): Flow<List<Person>>
    suspend fun getById(id: String): Person?

    /** Case- and whitespace-insensitive, so a typed name resolves to an existing person. */
    suspend fun findByName(name: String): Person?

    suspend fun insert(person: Person)
    suspend fun update(person: Person)

    /**
     * Hard delete. Prefer archiving: foreign keys are not enforced, so this orphans the
     * person's ledger entries rather than being refused.
     */
    suspend fun delete(id: String)
    suspend fun count(): Long
}
