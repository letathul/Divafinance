package com.divafinance.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one way a platform entry point hands a URI to the navigation graph.
 *
 * An object with process-wide state, like `IosPlatformBridges`, because the callers are
 * `MainActivity.onNewIntent` and `iOSApp.swift` — neither of which can reach into
 * composition or resolve a Koin scope. [MainScreen] collects [pending] and calls
 * [consume] once it has navigated, so a URI that arrives before the shell exists is
 * still honoured rather than dropped.
 */
object DeepLinks {

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun open(uri: String) {
        _pending.value = uri
    }

    fun consume() {
        _pending.value = null
    }
}
