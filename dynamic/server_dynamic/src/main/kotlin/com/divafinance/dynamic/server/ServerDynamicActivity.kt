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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.theme.DivaTheme

class ServerDynamicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DivaTheme {
                var running by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (running) "Server Running" else "Server Stopped",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (running) "Access at http://localhost:8080" else "Start the server to access from browser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    DivaButton(
                        text = if (running) "Stop Server" else "Start Server",
                        onClick = {
                            if (running) {
                                stopService(Intent(this@ServerDynamicActivity, ServerForegroundService::class.java))
                                running = false
                            } else {
                                val intent = Intent(this@ServerDynamicActivity, ServerForegroundService::class.java)
                                startForegroundService(intent)
                                running = true
                            }
                        },
                    )
                }
            }
        }
    }
}
