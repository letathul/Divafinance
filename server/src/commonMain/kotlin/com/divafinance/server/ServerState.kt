package com.divafinance.server

/**
 * Observable lifecycle of the embedded server. Binding a socket can fail (port already
 * taken, no permission), and it fails *after* `start()` has returned, so the UI has to
 * be told rather than asked.
 */
sealed interface ServerState {
    data object Stopped : ServerState

    data object Starting : ServerState

    /**
     * @param address LAN address the phone is reachable at, or null when it could not
     *   be resolved (no Wi-Fi, cellular only). The server is still running either way.
     */
    data class Running(val address: String?, val port: Int) : ServerState

    data class Failed(val message: String) : ServerState
}

/** True while the switch should read as "on" — including the brief bind window. */
val ServerState.isActive: Boolean
    get() = this is ServerState.Starting || this is ServerState.Running

/** The URL to type into a browser on the same network, once we know one. */
val ServerState.browserUrl: String?
    get() = (this as? ServerState.Running)?.let { "http://${it.address ?: "localhost"}:${it.port}" }
