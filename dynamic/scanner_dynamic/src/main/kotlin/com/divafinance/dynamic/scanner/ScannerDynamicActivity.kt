package com.divafinance.dynamic.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.feature.scanner.ScannerScreen

class ScannerDynamicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DivaTheme {
                ScannerScreen(onBack = { finish() })
            }
        }
    }
}
