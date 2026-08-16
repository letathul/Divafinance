package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.testing.fake.FakeAccountRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InitializeDatabaseUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val accountRepo = FakeAccountRepository()
    private val useCase = InitializeDatabaseUseCase(settingsRepo, accountRepo)

    @Test
    fun returnsTrueWhenOnboardingCompleted() = runTest {
        settingsRepo.set(UserSettings.KEY_ONBOARDING_COMPLETED, "true")

        val result = useCase()

        assertTrue(result)
    }

    @Test
    fun returnsFalseWhenOnboardingNotCompleted() = runTest {
        val result = useCase()

        assertFalse(result)
    }

    @Test
    fun returnsFalseForNonTrueValue() = runTest {
        settingsRepo.set(UserSettings.KEY_ONBOARDING_COMPLETED, "false")

        val result = useCase()

        assertFalse(result)
    }

    /**
     * Installs made before onboarding created a real account have transactions pointing at
     * the literal id "default" and no account row. They never re-run onboarding, so the
     * repair has to happen on launch.
     */
    @Test
    fun repairsLegacyInstallByCreatingTheMissingDefaultAccount() = runTest {
        settingsRepo.set(UserSettings.KEY_ONBOARDING_COMPLETED, "true")

        useCase()

        val account = accountRepo.getAll().first().single()
        assertEquals("default", account.id)
        assertEquals("default", settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID))
    }

    @Test
    fun repairedAccountUsesTheConfiguredBaseCurrency() = runTest {
        settingsRepo.set(UserSettings.KEY_BASE_CURRENCY, "GBP")

        useCase()

        assertEquals("GBP", accountRepo.getAll().first().single().currency)
    }

    @Test
    fun leavesAValidDefaultAccountAlone() = runTest {
        val existing = TestData.account(id = "acc-1")
        accountRepo.setAccounts(listOf(existing))
        settingsRepo.set(UserSettings.KEY_DEFAULT_ACCOUNT_ID, "acc-1")

        useCase()

        assertEquals(listOf(existing), accountRepo.getAll().first())
        assertEquals("acc-1", settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID))
    }

    @Test
    fun adoptsAnExistingLegacyAccountRatherThanDuplicatingIt() = runTest {
        accountRepo.setAccounts(listOf(TestData.account(id = "default", name = "Wallet")))

        useCase()

        val account = accountRepo.getAll().first().single()
        assertEquals("Wallet", account.name)
        assertEquals("default", settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID))
    }

    @Test
    fun isIdempotentAcrossLaunches() = runTest {
        useCase()
        useCase()
        useCase()

        assertEquals(1, accountRepo.getAll().first().size)
        assertNotNull(settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID))
    }

    /** A recorded id that no longer resolves must be repaired, not trusted. */
    @Test
    fun replacesADanglingDefaultAccountId() = runTest {
        settingsRepo.set(UserSettings.KEY_DEFAULT_ACCOUNT_ID, "gone")

        useCase()

        val id = assertNotNull(settingsRepo.get(UserSettings.KEY_DEFAULT_ACCOUNT_ID))
        assertNotNull(accountRepo.getById(id))
    }
}
