package com.divafinance.dynamic.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.divafinance.server.DivaServer
import com.divafinance.server.ServerConfig
import com.divafinance.server.ServerState
import com.divafinance.server.browserUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ServerForegroundService : Service() {

    private val divaServer: DivaServer by inject()
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Must happen within seconds of startForegroundService() or the OS kills us,
        // so the notification goes up before the server is asked to bind.
        startForegroundCompat(notification(getString(R.string.server_notification_starting)))
        observeServerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, ServerConfig().port) ?: ServerConfig().port
        divaServer.start(ServerConfig(port = port))
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        divaServer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Keeps the notification honest about the address and about failures to bind. */
    private fun observeServerState() {
        scope.launch {
            divaServer.state.collectLatest { state ->
                val text = when (state) {
                    is ServerState.Running -> state.browserUrl
                        ?: getString(R.string.server_notification_running_no_address)
                    is ServerState.Failed -> state.message
                    else -> getString(R.string.server_notification_starting)
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification(text))
                if (state is ServerState.Failed) stopSelf()
            }
        }
    }

    private fun notification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.server_notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.server_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.server_notification_channel_description)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_PORT = "com.divafinance.dynamic.server.PORT"
        private const val CHANNEL_ID = "diva_server_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
