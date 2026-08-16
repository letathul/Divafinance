package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.common.UuidGenerator
import com.divafinance.core.data.repository.AccountRepository
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.Account
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.AccountType
import kotlin.time.Clock

class InitializeDatabaseUseCase(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
) {
    /**
     * Runs on every launch. Returns whether onboarding has been completed.
     *
     * Also repairs installs created before onboarding started inserting a real account:
     * those have transactions referencing the literal id "default" with no matching row,
     * and no [UserSettings.KEY_DEFAULT_ACCOUNT_ID]. The repair is idempotent and cheap —
     * once a default account id resolves to a real row, it does nothing.
     */
    suspend operator fun invoke(): Boolean {
        ensureDefaultAccount()
        return settingsRepository.get(UserSettings.KEY_ONBOARDING_COMPLETED) == "true"
    }

    private suspend fun ensureDefaultAccount() {
        val recordedId = settingsRepository.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID)
        if (recordedId != null && accountRepository.getById(recordedId) != null) return

        // Adopt any existing account before creating another, so a user who set one up
        // through the cards flow or demo data does not end up with a stray duplicate.
        val existing = accountRepository.getById(LEGACY_ACCOUNT_ID)
        val accountId = when {
            existing != null -> existing.id
            else -> createLegacyAccount()
        }
        settingsRepository.set(UserSettings.KEY_DEFAULT_ACCOUNT_ID, accountId)
    }

    /**
     * Created with the literal id the old code hardcoded, so transactions already written
     * with `accountId = "default"` become valid rather than needing a rewrite.
     */
    private suspend fun createLegacyAccount(): String {
        val now = Clock.System.now()
        val currency = settingsRepository.get(UserSettings.KEY_BASE_CURRENCY) ?: "USD"
        accountRepository.insert(
            Account(
                id = LEGACY_ACCOUNT_ID,
                name = "Main Account",
                type = AccountType.CHECKING,
                currency = currency,
                createdAt = now,
                updatedAt = now,
            )
        )
        return LEGACY_ACCOUNT_ID
    }

    private companion object {
        /** The id [com.divafinance.core.model.Transaction.accountId] was hardcoded to. */
        const val LEGACY_ACCOUNT_ID = "default"
    }
}
