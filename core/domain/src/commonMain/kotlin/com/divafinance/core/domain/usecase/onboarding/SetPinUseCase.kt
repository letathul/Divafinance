package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.common.SecurityUtils
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings

class SetPinUseCase(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(pin: String) {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(pin, salt)
        settingsRepository.set(UserSettings.KEY_PIN_SALT, salt)
        settingsRepository.set(UserSettings.KEY_PIN_HASH, hash)
    }
}
