package com.divafinance.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Both bars are fully transparent. The scrims used to mirror the theme's
        // background hex by hand, which was both a maintenance trap — they resolve
        // before Compose runs and cannot read the theme — and subtly wrong, because
        // SystemBarStyle.auto keys off the *system* dark setting rather than the app's
        // own ThemeMode. DivaTabBar draws its own translucent fill to the screen edge,
        // so it is the navigation bar's background now.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Without this, API 29+ re-adds its own translucent scrim behind a transparent
        // navigation bar and double-darkens the bar we just drew.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            App()
        }
    }
}
