package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings

class InitializeDatabaseUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Boolean {
        val onboardingDone = settingsRepository.get(UserSettings.KEY_ONBOARDING_COMPLETED)
        return onboardingDone == "true"
    }
}
