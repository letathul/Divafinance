package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings

class ValidatePinUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(pinHash: String): Boolean {
        val storedHash = settingsRepository.get(UserSettings.KEY_PIN_HASH) ?: return false
        return storedHash == pinHash
    }
}
