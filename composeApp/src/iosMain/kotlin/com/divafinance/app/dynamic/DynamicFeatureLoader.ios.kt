package com.divafinance.app.dynamic

actual class DynamicFeatureLoader {
    actual fun isInstalled(module: DynamicModule): Boolean = true

    actual suspend fun requestInstall(
        module: DynamicModule,
        onProgress: (Float) -> Unit,
    ): Boolean = true
}
