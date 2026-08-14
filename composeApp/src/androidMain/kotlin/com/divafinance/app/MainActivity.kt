package com.divafinance.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.argb(0xe6, 0xFA, 0xFA, 0xFA),
                Color.argb(0xe6, 0x1E, 0x1E, 0x2E),
            ),
        )
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
