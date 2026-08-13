package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.domain.fake.FakeSettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitializeDatabaseUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val useCase = InitializeDatabaseUseCase(settingsRepo)

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
}
