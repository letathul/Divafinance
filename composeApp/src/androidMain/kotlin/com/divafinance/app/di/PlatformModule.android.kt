package com.divafinance.app.di

import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.core.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
    single { DynamicFeatureLoader(get()) }
}
