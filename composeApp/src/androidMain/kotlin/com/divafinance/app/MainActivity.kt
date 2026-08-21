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
            // Scrims mirror LightBackground / DarkBackground in core:ui. They are
            // resolved by the system before Compose runs, so they cannot read the theme
            // and have to be kept in step with the palette by hand.
            navigationBarStyle = SystemBarStyle.auto(
                Color.argb(0xe6, 0xFB, 0xFA, 0xFC),
                Color.argb(0xe6, 0x0B, 0x09, 0x0D),
            ),
        )
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
