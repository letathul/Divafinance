package com.divafinance.dynamic.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.feature.map.SpendingMapScreen

class MapDynamicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DivaTheme {
                SpendingMapScreen(onBack = { finish() })
            }
        }
    }
}
