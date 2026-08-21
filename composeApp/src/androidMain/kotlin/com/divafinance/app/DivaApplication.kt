package com.divafinance.app

import android.app.Application
import android.content.Context
import com.divafinance.app.di.initKoin
import com.google.android.play.core.splitcompat.SplitCompat
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class DivaApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Without this, code from an on-demand split (the server's foreground service)
        // is not on the classloader until the app restarts.
        SplitCompat.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@DivaApplication)
        }
    }
}
