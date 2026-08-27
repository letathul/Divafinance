package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class PersonRepositoryImpl(
    private val db: DivaFinanceDb
) : PersonRepository {

    override fun getAll(): Flow<List<Person>> {
        return db.personQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun getActive(): Flow<List<Person>> {
        return db.personQueries.selectActive()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Person? =
        db.personQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun findByName(name: String): Person? =
        db.personQueries.selectByName(name).executeAsOneOrNull()?.toDomain()

    override suspend fun insert(person: Person) {
        db.personQueries.insert(
            id = person.id,
            name = person.name,
            note = person.note,
            is_archived = if (person.isArchived) 1L else 0L,
            created_at = person.createdAt.toString(),
            updated_at = person.updatedAt.toString(),
            color_hex = person.colorHex,
        )
    }

    override suspend fun update(person: Person) {
        db.personQueries.update(
            name = person.name,
            note = person.note,
            is_archived = if (person.isArchived) 1L else 0L,
            updated_at = person.updatedAt.toString(),
            color_hex = person.colorHex,
            id = person.id,
        )
    }

    override suspend fun delete(id: String) {
        db.personQueries.delete(id)
    }

    override suspend fun count(): Long = db.personQueries.count().executeAsOne()
}

private fun com.divafinance.core.database.Person.toDomain() = Person(
    id = id,
    name = name,
    note = note,
    colorHex = color_hex,
    isArchived = is_archived == 1L,
    createdAt = Instant.parse(created_at),
    updatedAt = Instant.parse(updated_at),
)
