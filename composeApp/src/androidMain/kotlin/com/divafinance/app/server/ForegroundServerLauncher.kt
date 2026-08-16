package com.divafinance.app.server

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.app.dynamic.DynamicModule
import com.divafinance.server.DivaServer
import com.divafinance.server.ServerConfig
import com.divafinance.server.ServerLauncher

/**
 * Runs the embedded server inside a foreground service so Android does not reclaim the
 * process the moment the user leaves the app — otherwise the browser tab they just
 * opened on their laptop goes dead within seconds.
 *
 * The service lives in the on-demand `server_dynamic` split, so it is referenced by
 * name rather than by class literal: the base APK cannot link against a module that may
 * not be installed. When the split is absent we still start the server in-process, which
 * works for as long as the app is on screen.
 */
class ForegroundServerLauncher(
    private val context: Context,
    private val divaServer: DivaServer,
    private val dynamicFeatureLoader: DynamicFeatureLoader,
) : ServerLauncher {

    override fun start(config: ServerConfig) {
        if (!dynamicFeatureLoader.isInstalled(DynamicModule.SERVER)) {
            divaServer.start(config)
            return
        }
        val started = runCatching {
            ContextCompat.startForegroundService(context, serviceIntent().putExtra(EXTRA_PORT, config.port))
        }.isSuccess
        if (!started) divaServer.start(config)
    }

    override fun stop() {
        runCatching { context.stopService(serviceIntent()) }
        // Also stop directly: covers the in-process fallback above, and makes stopping
        // idempotent when the service was never running.
        divaServer.stop()
    }

    private fun serviceIntent() = Intent().setClassName(context.packageName, SERVICE_CLASS)

    private companion object {
        const val SERVICE_CLASS = "com.divafinance.dynamic.server.ServerForegroundService"
        const val EXTRA_PORT = "com.divafinance.dynamic.server.PORT"
    }
}
