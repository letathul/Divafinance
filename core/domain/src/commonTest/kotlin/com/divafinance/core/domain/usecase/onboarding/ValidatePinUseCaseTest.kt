package com.divafinance.core.domain.usecase.onboarding

import com.divafinance.core.common.SecurityUtils
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatePinUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val useCase = ValidatePinUseCase(settingsRepo)

    private suspend fun storePin(pin: String): String {
        val salt = SecurityUtils.generateSalt()
        settingsRepo.set(UserSettings.KEY_PIN_SALT, salt)
        settingsRepo.set(UserSettings.KEY_PIN_HASH, SecurityUtils.hashPin(pin, salt))
        return salt
    }

    @Test
    fun returnsTrueForMatchingPin() = runTest {
        storePin("1234")

        val result = useCase("1234")

        assertTrue(result)
    }

    @Test
    fun returnsFalseForMismatch() = runTest {
        storePin("1234")

        val result = useCase("9999")

        assertFalse(result)
    }

    @Test
    fun returnsFalseWhenNoPinSet() = runTest {
        val result = useCase("1234")

        assertFalse(result)
    }

    @Test
    fun returnsFalseWhenSaltMissing() = runTest {
        settingsRepo.set(UserSettings.KEY_PIN_HASH, "somestoredhash")

        val result = useCase("1234")

        assertFalse(result)
    }
}
