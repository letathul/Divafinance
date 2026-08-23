package com.divafinance.app.di

import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.app.llm.FoundationModelReceiptExtractor
import com.divafinance.app.llm.IosPlatformBridges
import com.divafinance.core.domain.usecase.scanner.ReceiptExtractor
import com.divafinance.core.domain.usecase.scanner.UnavailableReceiptExtractor
import com.divafinance.core.common.FileSystem
import com.divafinance.core.common.LocationProvider
import com.divafinance.core.common.LocationSource
import com.divafinance.core.database.DatabaseDriverFactory
import com.divafinance.feature.automation.IosShortcutRegistrar
import com.divafinance.feature.automation.ShortcutRegistrar
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
    single<LocationSource> { LocationProvider() }
    single<ShortcutRegistrar> { IosShortcutRegistrar() }
    // Present only when the Swift side handed one over, which it does on an iOS with Apple
    // Intelligence and not otherwise. The unavailable default is not a degraded mode — the
    // rules-based parser is the primary path on every platform.
    single<ReceiptExtractor> {
        IosPlatformBridges.foundationModel
            ?.let(::FoundationModelReceiptExtractor)
            ?: UnavailableReceiptExtractor
    }
    // No split delivery and no foreground services on iOS — the app process is the
    // server's whole lifetime.
    single<ServerLauncher> { InProcessServerLauncher(get()) }
    single<LocalAddressResolver> { NoLocalAddressResolver }
}
