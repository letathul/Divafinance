package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.data.repository.AccountRepository
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.Account
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.AccountType
import kotlin.time.Clock

class CompleteOnboardingUseCase(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
) {
    /**
     * Onboarding used to collect an account name and type and then throw them away, leaving
     * every transaction pointing at an `accountId` of "default" that no row ever matched.
     * The account is now created for real and its id recorded as the default.
     */
    suspend operator fun invoke(
        baseCurrency: String,
        defaultLocation: String?,
        accountName: String = "",
        accountType: String = AccountType.CHECKING.name,
    ) {
        val now = Clock.System.now()
        val account = Account(
            id = UuidGenerator.generate(),
            name = accountName.ifBlank { "Main Account" },
            type = AccountType.entries.firstOrNull { it.name == accountType }
                ?: AccountType.CHECKING,
            currency = baseCurrency,
            createdAt = now,
            updatedAt = now,
        )
        accountRepository.insert(account)

        settingsRepository.set(UserSettings.KEY_ONBOARDING_COMPLETED, "true")
        settingsRepository.set(UserSettings.KEY_BASE_CURRENCY, baseCurrency)
        settingsRepository.set(UserSettings.KEY_DEFAULT_ACCOUNT_ID, account.id)
        defaultLocation?.let {
            settingsRepository.set(UserSettings.KEY_DEFAULT_LOCATION, it)
        }
    }
}
