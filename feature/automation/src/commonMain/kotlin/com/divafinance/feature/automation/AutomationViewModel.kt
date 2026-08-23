package com.divafinance.feature.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AutomationAction(
    val id: String,
    val title: String,
    val description: String,
    val platform: AutomationPlatform,
    val enabled: Boolean = false,
)

enum class AutomationPlatform { ANDROID, IOS, CROSS_PLATFORM }

data class AutomationUiState(
    val actions: List<AutomationAction> = defaultActions(),
)

class AutomationViewModel(
    private val shortcutRegistrar: ShortcutRegistrar,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    init {
        // Re-register on every construction rather than only on a toggle. The OS keeps
        // shortcuts across launches and upgrades, so the stored set is the only thing that
        // says what they should be — and a reinstall drops them while the setting survives.
        viewModelScope.launch {
            val enabled = settingsRepository.get(UserSettings.KEY_ENABLED_AUTOMATIONS)
                ?.split(',')
                ?.filter { it.isNotBlank() }
                .orEmpty()
                .toSet()
            _uiState.value = _uiState.value.copy(
                actions = _uiState.value.actions.map { it.copy(enabled = it.id in enabled) },
            )
            syncShortcuts()
        }
    }

    fun toggleAction(id: String) {
        _uiState.value = _uiState.value.copy(
            actions = _uiState.value.actions.map { action ->
                if (action.id == id) action.copy(enabled = !action.enabled) else action
            },
        )
        syncShortcuts()
        viewModelScope.launch {
            settingsRepository.set(
                UserSettings.KEY_ENABLED_AUTOMATIONS,
                enabledActions().joinToString(",") { it.id },
            )
        }
    }

    /** The URI a shortcut carries, and the single place its format is defined. */
    private fun deepLinkFor(id: String) = "divafinance://automation/$id"

    private fun enabledActions() = _uiState.value.actions.filter { it.enabled }

    private fun syncShortcuts() {
        val enabledShortcuts = enabledActions().map { action ->
            ShortcutInfo(
                id = action.id,
                title = action.title,
                description = action.description,
                deepLinkUri = deepLinkFor(action.id),
            )
        }
        shortcutRegistrar.registerShortcuts(enabledShortcuts)
    }
}

internal fun defaultActions() = listOf(
    AutomationAction(
        id = "quick_expense",
        title = "Quick Add Expense",
        description = "Add an expense directly from a shortcut or widget",
        platform = AutomationPlatform.CROSS_PLATFORM,
    ),
    AutomationAction(
        id = "daily_summary",
        title = "Daily Spending Summary",
        description = "Get a notification with your daily spending at a set time",
        platform = AutomationPlatform.CROSS_PLATFORM,
    ),
    AutomationAction(
        id = "budget_alert",
        title = "Budget Threshold Alert",
        description = "Get notified when spending in a category exceeds the threshold",
        platform = AutomationPlatform.CROSS_PLATFORM,
    ),
    AutomationAction(
        id = "android_widget",
        title = "Home Screen Widget",
        description = "Show balance and recent transactions on the home screen",
        platform = AutomationPlatform.ANDROID,
    ),
    AutomationAction(
        id = "android_nfc",
        title = "NFC Tag Trigger",
        description = "Tap an NFC tag to quickly log a recurring expense",
        platform = AutomationPlatform.ANDROID,
    ),
    AutomationAction(
        id = "ios_siri",
        title = "Siri Shortcut",
        description = "Use Siri to add expenses or check your balance",
        platform = AutomationPlatform.IOS,
    ),
    AutomationAction(
        id = "ios_shortcuts",
        title = "Shortcuts Integration",
        description = "Use Apple Shortcuts to automate expense logging",
        platform = AutomationPlatform.IOS,
    ),
)
