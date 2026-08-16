package com.divafinance.server

/**
 * How the embedded server is brought up on a given platform.
 *
 * Android has to route through a foreground service, otherwise the OS reclaims the
 * process the moment the user leaves the app and the browser tab on their laptop dies
 * with it. Everywhere else the in-process server is the whole story.
 */
interface ServerLauncher {
    fun start(config: ServerConfig)
    fun stop()
}

/** Starts [DivaServer] directly in the current process. */
class InProcessServerLauncher(private val server: DivaServer) : ServerLauncher {
    override fun start(config: ServerConfig) = server.start(config)
    override fun stop() = server.stop()
}
