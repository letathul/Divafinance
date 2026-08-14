package com.divafinance.dynamic.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.divafinance.server.DivaServer
import com.divafinance.server.ServerConfig
import org.koin.android.ext.android.inject

class ServerForegroundService : Service() {

    private val divaServer: DivaServer by inject()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Diva Finance Server")
            .setContentText("Running on port 8080")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        divaServer.start(ServerConfig())
        return START_STICKY
    }

    override fun onDestroy() {
        divaServer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Diva Server",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Local web server for browser access"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "diva_server_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
