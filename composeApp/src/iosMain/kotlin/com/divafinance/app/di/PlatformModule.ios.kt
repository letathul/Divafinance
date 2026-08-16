package com.divafinance.app.di

import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.core.common.FileSystem
import com.divafinance.core.database.DatabaseDriverFactory
import com.divafinance.server.InProcessServerLauncher
import com.divafinance.server.LocalAddressResolver
import com.divafinance.server.NoLocalAddressResolver
import com.divafinance.server.ServerLauncher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single { DynamicFeatureLoader() }
    single { FileSystem() }
    // No split delivery and no foreground services on iOS — the app process is the
    // server's whole lifetime.
    single<ServerLauncher> { InProcessServerLauncher(get()) }
    single<LocalAddressResolver> { NoLocalAddressResolver }
}
