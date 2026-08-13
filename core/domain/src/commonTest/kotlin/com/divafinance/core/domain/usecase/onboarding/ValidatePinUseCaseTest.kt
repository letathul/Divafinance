package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.domain.fake.FakeSettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatePinUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val useCase = ValidatePinUseCase(settingsRepo)

    @Test
    fun returnsTrueForMatchingHash() = runTest {
        settingsRepo.set(UserSettings.KEY_PIN_HASH, "abc123hash")

        val result = useCase("abc123hash")

        assertTrue(result)
    }

    @Test
    fun returnsFalseForMismatch() = runTest {
        settingsRepo.set(UserSettings.KEY_PIN_HASH, "abc123hash")

        val result = useCase("wronghash")

        assertFalse(result)
    }

    @Test
    fun returnsFalseWhenNoPinSet() = runTest {
        val result = useCase("anyhash")

        assertFalse(result)
    }
}
