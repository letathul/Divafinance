package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.common.SecurityUtils
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings

class ValidatePinUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(pin: String): Boolean {
        val storedHash = settingsRepository.get(UserSettings.KEY_PIN_HASH) ?: return false
        val storedSalt = settingsRepository.get(UserSettings.KEY_PIN_SALT) ?: return false
        val inputHash = SecurityUtils.hashPin(pin, storedSalt)
        return storedHash == inputHash
    }
}
