package com.divafinance.app

import androidx.compose.ui.window.ComposeUIViewController
import com.divafinance.app.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
