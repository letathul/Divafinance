package com.divafinance.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.model.UserSettings
import com.divafinance.feature.demo.DemoDataManager
import com.divafinance.feature.demo.DemoStatus
import com.divafinance.server.DivaServer
import com.divafinance.server.ServerConfig
import com.divafinance.server.ServerLauncher
import com.divafinance.server.ServerState
import com.divafinance.server.browserUrl
import com.divafinance.server.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverState: ServerState = ServerState.Stopped,
    val serverPort: Int = ServerConfig().port,
    val currency: String = "USD",
    val isDarkTheme: Boolean = false,
    /** The demo section only exists while the demo is active; removal is one-way. */
    val isDemoActive: Boolean = false,
    val isRemovingDemo: Boolean = false,
) {
    val isServerRunning: Boolean get() = serverState.isActive

    /** Non-null only once the socket is bound — this is what the user types in a browser. */
    val serverUrl: String? get() = serverState.browserUrl

    val serverError: String? get() = (serverState as? ServerState.Failed)?.message
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val divaServer: DivaServer,
    private val serverLauncher: ServerLauncher,
    private val demoDataManager: DemoDataManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeServer()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val currency = settingsRepository.get("currency") ?: "USD"
            val darkTheme = settingsRepository.get("dark_theme") == "true"
            val port = settingsRepository.get(UserSettings.KEY_SERVER_PORT)?.toIntOrNull()
                ?: ServerConfig().port
            _uiState.update {
                it.copy(
                    currency = currency,
                    isDarkTheme = darkTheme,
                    serverPort = port,
                    isDemoActive = demoDataManager.status() == DemoStatus.ACTIVE,
                )
            }
        }
    }

    /**
     * The server binds off-thread and can fail after the switch has been flipped, so the
     * UI follows the server's own state rather than the user's intent.
     */
    private fun observeServer() {
        viewModelScope.launch {
            divaServer.state.collect { state ->
                _uiState.update { it.copy(serverState = state) }
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
            serverLauncher.start(ServerConfig(port = _uiState.value.serverPort))
        } else {
            serverLauncher.stop()
        }
    }

    fun updateServerPort(port: Int) {
        if (port !in MIN_PORT..MAX_PORT) return
        viewModelScope.launch {
            settingsRepository.set(UserSettings.KEY_SERVER_PORT, port.toString())
            _uiState.update { it.copy(serverPort = port) }
        }
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

    private companion object {
        // Below 1024 needs root on Android; the server would never bind.
        const val MIN_PORT = 1024
        const val MAX_PORT = 65535
    }
}
