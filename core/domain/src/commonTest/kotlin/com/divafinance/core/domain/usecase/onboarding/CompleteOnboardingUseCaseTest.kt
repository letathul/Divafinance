package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.testing.fake.FakeAccountRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.AccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CompleteOnboardingUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val accountRepo = FakeAccountRepository()
    private val useCase = CompleteOnboardingUseCase(settingsRepo, accountRepo)

    @Test
    fun setsOnboardingCompleted() = runTest {
        useCase("USD", null)

        assertEquals("true", settingsRepo.get(UserSettings.KEY_ONBOARDING_COMPLETED))
    }

    @Test
    fun setsBaseCurrency() = runTest {
        useCase("EUR", null)

        assertEquals("EUR", settingsRepo.get(UserSettings.KEY_BASE_CURRENCY))
    }

    @Test
    fun setsDefaultLocation() = runTest {
        useCase("USD", "New York")

        assertEquals("New York", settingsRepo.get(UserSettings.KEY_DEFAULT_LOCATION))
    }

    @Test
    fun skipsLocationWhenNull() = runTest {
        useCase("USD", null)

        assertNull(settingsRepo.get(UserSettings.KEY_DEFAULT_LOCATION))
    }

    @Test
    fun createsAccountAndRecordsItAsDefault() = runTest {
        useCase("EUR", null, accountName = "Everyday", accountType = "SAVINGS")

        val account = accountRepo.getAll().first().single()
        assertEquals("Everyday", account.name)
        assertEquals(AccountType.SAVINGS, account.type)
        assertEquals("EUR", account.currency)
        assertEquals(account.id, settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID))
    }

    @Test
    fun namesAccountWhenNameIsBlank() = runTest {
        useCase("USD", null, accountName = "  ")

        assertEquals("Main Account", accountRepo.getAll().first().single().name)
    }

    @Test
    fun fallsBackToCheckingForUnknownAccountType() = runTest {
        useCase("USD", null, accountType = "NOT_A_TYPE")

        assertEquals(AccountType.CHECKING, accountRepo.getAll().first().single().type)
    }

    @Test
    fun defaultAccountIdResolvesToARealAccount() = runTest {
        useCase("USD", null)

        val id = settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID)
        assertNotNull(id)
        assertNotNull(accountRepo.getById(id))
    }
}
