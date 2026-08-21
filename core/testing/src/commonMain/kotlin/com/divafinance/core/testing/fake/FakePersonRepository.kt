package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePersonRepository : PersonRepository {
    private val people = MutableStateFlow<List<Person>>(emptyList())

    override fun getAll(): Flow<List<Person>> = people

    override fun getActive(): Flow<List<Person>> =
        people.map { list -> list.filterNot { it.isArchived } }

    override suspend fun getById(id: String): Person? = people.value.find { it.id == id }

    // Mirrors the SQL's LOWER(TRIM(...)) matching, so tests exercise the real semantics.
    override suspend fun findByName(name: String): Person? =
        people.value.find { it.name.trim().equals(name.trim(), ignoreCase = true) }

    override suspend fun insert(person: Person) {
        people.value = people.value + person
    }

    override suspend fun update(person: Person) {
        people.value = people.value.map { if (it.id == person.id) person else it }
    }

    override suspend fun delete(id: String) {
        people.value = people.value.filterNot { it.id == id }
    }

    override suspend fun count(): Long = people.value.size.toLong()

    fun setPeople(list: List<Person>) {
        people.value = list
    }
}
