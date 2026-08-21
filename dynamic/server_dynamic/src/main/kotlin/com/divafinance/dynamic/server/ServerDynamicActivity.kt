package com.divafinance.dynamic.server

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.server.DivaServer
import com.divafinance.server.ServerState
import com.divafinance.server.browserUrl
import com.divafinance.server.isActive
import org.koin.android.ext.android.inject

class ServerDynamicActivity : ComponentActivity() {

    private val divaServer: DivaServer by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DivaTheme {
                // Driven by the server's own state, not by a local flag: binding happens
                // off-thread and can fail, and the service can also be stopped elsewhere.
                val state by divaServer.state.collectAsState()
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when (state) {
                            is ServerState.Running -> "Server Running"
                            ServerState.Starting -> "Starting…"
                            is ServerState.Failed -> "Server Failed"
                            ServerState.Stopped -> "Server Stopped"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (val current = state) {
                            is ServerState.Failed -> current.message
                            else -> state.browserUrl?.let { "Access at $it" }
                                ?: "Start the server to access from browser"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    DivaButton(
                        text = if (state.isActive) "Stop Server" else "Start Server",
                        onClick = {
                            val intent = Intent(this@ServerDynamicActivity, ServerForegroundService::class.java)
                            if (state.isActive) {
                                stopService(intent)
                            } else {
                                ContextCompat.startForegroundService(this@ServerDynamicActivity, intent)
                            }
                        },
                    )
                }
            }
        }
    }
}
