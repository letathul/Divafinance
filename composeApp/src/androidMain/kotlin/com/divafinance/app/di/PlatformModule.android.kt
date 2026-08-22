package com.divafinance.app.di

import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.app.server.ForegroundServerLauncher
import com.divafinance.core.common.FileSystem
import com.divafinance.core.common.LocationProvider
import com.divafinance.core.common.LocationSource
import com.divafinance.core.database.DatabaseDriverFactory
import com.divafinance.core.domain.usecase.scanner.ReceiptExtractor
import com.divafinance.core.domain.usecase.scanner.UnavailableReceiptExtractor
import com.divafinance.server.AndroidLocalAddressResolver
import com.divafinance.server.LocalAddressResolver
import com.divafinance.server.ServerLauncher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
    single { DynamicFeatureLoader(get()) }
    single { FileSystem(get()) }
    single<LocationSource> { LocationProvider(get()) }
    single<ServerLauncher> { ForegroundServerLauncher(get(), get(), get()) }
    single<LocalAddressResolver> { AndroidLocalAddressResolver(get()) }
    // No system language model is reachable here yet: the AI Edge prompt API for Gemini Nano
    // is still limited-access and restricted to a handful of devices, and bundling weights
    // would undo the point of shipping OCR itself as an on-demand split. Scanning on Android
    // is the rules-based parser, which is the primary path on every platform anyway.
    single<ReceiptExtractor> { UnavailableReceiptExtractor }
}
