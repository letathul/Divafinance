package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.domain.fake.FakeSettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompleteOnboardingUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val useCase = CompleteOnboardingUseCase(settingsRepo)

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
}
