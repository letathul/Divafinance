package com.divafinance.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.feature.demo.DemoDataManager
import com.divafinance.feature.demo.DemoStatus
import com.divafinance.server.DivaServer
import com.divafinance.server.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isServerRunning: Boolean = false,
    val serverPort: Int = 8080,
    val currency: String = "USD",
    val isDarkTheme: Boolean = false,
    /** The demo section only exists while the demo is active; removal is one-way. */
    val isDemoActive: Boolean = false,
    val isRemovingDemo: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val divaServer: DivaServer,
    private val demoDataManager: DemoDataManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val currency = settingsRepository.get("currency") ?: "USD"
            val darkTheme = settingsRepository.get("dark_theme") == "true"
            val port = settingsRepository.get("server_port")?.toIntOrNull() ?: 8080
            _uiState.update {
                it.copy(
                    currency = currency,
                    isDarkTheme = darkTheme,
                    serverPort = port,
                    isServerRunning = divaServer.isRunning(),
                    isDemoActive = demoDataManager.status() == DemoStatus.ACTIVE,
                )
            }
        }
    }

    /**
     * Deletes the demo data and retires the offer for good. The section disappears on
     * success and never returns.
     */
    fun removeDemoData() {
        if (_uiState.value.isRemovingDemo) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRemovingDemo = true) }
            demoDataManager.clear()
            _uiState.update { it.copy(isRemovingDemo = false, isDemoActive = false) }
        }
    }

    fun toggleServer(enabled: Boolean) {
        if (enabled) {
            divaServer.start(ServerConfig(port = _uiState.value.serverPort))
        } else {
            divaServer.stop()
        }
        _uiState.update { it.copy(isServerRunning = enabled) }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.set("currency", currency)
            _uiState.update { it.copy(currency = currency) }
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.set("dark_theme", enabled.toString())
            _uiState.update { it.copy(isDarkTheme = enabled) }
        }
    }
}
