package com.divafinance.core.data.repository

import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAll(): Flow<List<UserSettings>>
    suspend fun get(key: String): String?
    suspend fun set(key: String, value: String)
    suspend fun delete(key: String)
    suspend fun deleteAll()
}
