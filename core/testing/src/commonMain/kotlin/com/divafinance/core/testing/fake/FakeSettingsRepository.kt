package com.divafinance.core.testing.fake

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSettingsRepository : SettingsRepository {
    private val settings = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun getAll(): Flow<List<UserSettings>> =
        settings.map { map -> map.entries.map { UserSettings(it.key, it.value) } }

    override suspend fun get(key: String): String? = settings.value[key]

    override suspend fun set(key: String, value: String) {
        settings.value = settings.value + (key to value)
    }

    override suspend fun delete(key: String) {
        settings.value = settings.value - key
    }

    override suspend fun deleteAll() {
        settings.value = emptyMap()
    }

    fun setSettings(map: Map<String, String>) {
        settings.value = map
    }
}
