package com.divafinance.feature.demo

/**
 * Delivery of the on-demand `demo-dynamic` module.
 *
 * Declared here rather than in the app module so [DemoDataManager] can drive install
 * and uninstall without depending on the Play Core APIs (or on the app module, which
 * would be a dependency cycle). The app module supplies the real implementation.
 */
interface DemoModuleInstaller {
    suspend fun install(onProgress: (Float) -> Unit = {}): Boolean
    suspend fun uninstall(): Boolean

    /** Used on platforms without on-demand delivery, and in tests. */
    object NoOp : DemoModuleInstaller {
        override suspend fun install(onProgress: (Float) -> Unit): Boolean = true
        override suspend fun uninstall(): Boolean = true
    }
}
