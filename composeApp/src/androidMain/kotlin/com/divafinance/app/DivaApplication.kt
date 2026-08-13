package com.divafinance.app

import android.app.Application
import com.divafinance.app.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class DivaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@DivaApplication)
        }
    }
}
