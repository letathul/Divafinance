package com.divafinance.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.feature.settings.Appearance
import com.divafinance.feature.settings.AppearanceStore
import org.koin.compose.koinInject

@Composable
fun App() {
    val store = koinInject<AppearanceStore>()
    // Collected at the root so a change in Settings repaints the whole tree at once.
    val appearance by remember(store) { store.appearance }
        .collectAsState(initial = Appearance())

    DivaTheme(mode = appearance.mode, accent = appearance.accent) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen()
        }
    }
}
