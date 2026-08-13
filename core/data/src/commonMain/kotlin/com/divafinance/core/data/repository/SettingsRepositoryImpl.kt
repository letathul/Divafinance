package com.divafinance.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val db: DivaFinanceDb
) : SettingsRepository {

    override fun getAll(): Flow<List<UserSettings>> {
        return db.settingsQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { UserSettings(key = it.key, value = it.value_) }
            }
    }

    override suspend fun get(key: String): String? {
        return db.settingsQueries.selectByKey(key).executeAsOneOrNull()
    }

    override suspend fun set(key: String, value: String) {
        db.settingsQueries.upsert(key, value)
    }

    override suspend fun delete(key: String) {
        db.settingsQueries.delete(key)
    }

    override suspend fun deleteAll() {
        db.settingsQueries.deleteAll()
    }
}
