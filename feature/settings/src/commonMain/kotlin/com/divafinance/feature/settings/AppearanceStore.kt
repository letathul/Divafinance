package com.divafinance.feature.settings

import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings
import com.divafinance.core.ui.theme.AccentTheme
import com.divafinance.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the app root needs to know to paint itself. */
data class Appearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentTheme = AccentTheme.SUNSET,
)

/**
 * Reads the appearance preference as a stream.
 *
 * Deliberately built on [SettingsRepository.getAll], the one reactive read on that
 * interface — `get(key)` is a one-shot suspend, so a theme derived from it would only
 * change on the next launch. Going through the flow is what makes the switch in Settings
 * repaint the app immediately.
 */
class AppearanceStore(private val settingsRepository: SettingsRepository) {

    val appearance: Flow<Appearance> = settingsRepository.getAll().map { rows ->
        val values = rows.associate { it.key to it.value }
        Appearance(
            mode = when {
                values.containsKey(UserSettings.KEY_THEME_MODE) ->
                    ThemeMode.fromName(values[UserSettings.KEY_THEME_MODE])
                // Pre-ThemeMode installs stored a bare boolean. Honour it once so nobody
                // gets bounced back to the system theme by upgrading.
                values[UserSettings.KEY_LEGACY_DARK_THEME] == "true" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            accent = AccentTheme.fromName(values[UserSettings.KEY_ACCENT]),
        )
    }

    suspend fun setMode(mode: ThemeMode) {
        settingsRepository.set(UserSettings.KEY_THEME_MODE, mode.name)
    }

    suspend fun setAccent(accent: AccentTheme) {
        settingsRepository.set(UserSettings.KEY_ACCENT, accent.name)
    }
}
