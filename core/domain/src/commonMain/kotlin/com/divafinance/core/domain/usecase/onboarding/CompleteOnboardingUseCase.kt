package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings

class CompleteOnboardingUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        baseCurrency: String,
        defaultLocation: String?,
    ) {
        settingsRepository.set(UserSettings.KEY_ONBOARDING_COMPLETED, "true")
        settingsRepository.set(UserSettings.KEY_BASE_CURRENCY, baseCurrency)
        defaultLocation?.let {
            settingsRepository.set(UserSettings.KEY_DEFAULT_LOCATION, it)
        }
    }
}
