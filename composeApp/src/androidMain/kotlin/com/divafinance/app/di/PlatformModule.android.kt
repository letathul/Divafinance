package com.divafinance.app.di

import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.app.server.ForegroundServerLauncher
import com.divafinance.core.common.FileSystem
import com.divafinance.core.database.DatabaseDriverFactory
import com.divafinance.server.AndroidLocalAddressResolver
import com.divafinance.server.LocalAddressResolver
import com.divafinance.server.ServerLauncher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
    single { DynamicFeatureLoader(get()) }
    single { FileSystem(get()) }
    single<ServerLauncher> { ForegroundServerLauncher(get(), get(), get()) }
    single<LocalAddressResolver> { AndroidLocalAddressResolver(get()) }
}
