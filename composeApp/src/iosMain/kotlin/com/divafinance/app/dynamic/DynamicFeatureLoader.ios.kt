package com.divafinance.app.dynamic

/**
 * iOS ships as a single binary — there is no on-demand module delivery, so every
 * feature is always "installed" and uninstall is a no-op that reports success.
 */
actual class DynamicFeatureLoader {
    actual fun isInstalled(module: DynamicModule): Boolean = true

    actual suspend fun requestInstall(
        module: DynamicModule,
        onProgress: (Float) -> Unit,
    ): Boolean = true

    actual suspend fun requestUninstall(module: DynamicModule): Boolean = true
}
