package com.divafinance.app.di

import com.divafinance.app.dynamic.DynamicFeatureLoader
import com.divafinance.app.dynamic.DynamicModule
import com.divafinance.feature.demo.DemoModuleInstaller
import org.koin.dsl.module

val dynamicFeatureModule = module {
    single { DynamicFeatureAvailability(get()) }
    single<DemoModuleInstaller> { PlayDemoModuleInstaller(get()) }
}

/**
 * Bridges [DemoModuleInstaller] — declared in :feature:demo so it has no Play Core
 * dependency — onto the app's split-install loader.
 */
class PlayDemoModuleInstaller(
    private val loader: DynamicFeatureLoader,
) : DemoModuleInstaller {
    override suspend fun install(onProgress: (Float) -> Unit): Boolean =
        loader.requestInstall(DynamicModule.DEMO, onProgress)

    override suspend fun uninstall(): Boolean =
        loader.requestUninstall(DynamicModule.DEMO)
}

class DynamicFeatureAvailability(private val loader: DynamicFeatureLoader) {
    fun isMapAvailable(): Boolean = loader.isInstalled(DynamicModule.MAP)
    fun isScannerAvailable(): Boolean = loader.isInstalled(DynamicModule.SCANNER)
    fun isServerAvailable(): Boolean = loader.isInstalled(DynamicModule.SERVER)

    suspend fun installMap(onProgress: (Float) -> Unit = {}): Boolean =
        loader.requestInstall(DynamicModule.MAP, onProgress)

    suspend fun installScanner(onProgress: (Float) -> Unit = {}): Boolean =
        loader.requestInstall(DynamicModule.SCANNER, onProgress)

    suspend fun installServer(onProgress: (Float) -> Unit = {}): Boolean =
        loader.requestInstall(DynamicModule.SERVER, onProgress)
}
