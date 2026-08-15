package com.divafinance.dynamic.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.feature.demo.DemoTourScreen

class DemoDynamicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DivaTheme {
                DemoTourScreen(onBack = { finish() })
            }
        }
    }
}
