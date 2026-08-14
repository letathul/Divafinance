package com.divafinance.server.auth

import com.divafinance.core.common.SecurityUtils
import com.divafinance.core.data.repository.SettingsRepository

class PinAuthProvider(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun validate(pin: String): Boolean {
        val storedHash = settingsRepository.get("pin_hash") ?: return false
        val storedSalt = settingsRepository.get("pin_salt") ?: return false
        val hash = SecurityUtils.hashPin(pin, storedSalt)
        return hash == storedHash
    }
}
