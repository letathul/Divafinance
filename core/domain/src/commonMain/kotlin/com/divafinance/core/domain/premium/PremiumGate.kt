package com.divafinance.core.domain.premium

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Whether paid features are unlocked.
 *
 * There is no billing integration — this reads a local flag and nothing in the app sets it
 * yet. It exists so premium features are written against a boundary from the start;
 * swapping in Play Billing later means one new implementation rather than edits at every
 * call site.
 */
interface PremiumGate {
    val isPremium: Flow<Boolean>
    suspend fun isPremiumNow(): Boolean
}

class SettingsPremiumGate(
    private val settingsRepository: SettingsRepository,
) : PremiumGate {

    // Only getAll() is a Flow on SettingsRepository; get() is a one-shot suspend, so a
    // reactive read has to come off the full map.
    override val isPremium: Flow<Boolean> =
        settingsRepository.getAll().map { settings ->
            settings.any { it.key == UserSettings.KEY_IS_PREMIUM && it.value.toBoolean() }
        }

    override suspend fun isPremiumNow(): Boolean =
        settingsRepository.get(UserSettings.KEY_IS_PREMIUM)?.toBoolean() == true
}
