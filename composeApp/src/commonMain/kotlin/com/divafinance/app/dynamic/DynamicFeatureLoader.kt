package com.divafinance.app.dynamic

enum class DynamicModule(val moduleName: String) {
    MAP("map-dynamic"),
    SCANNER("scanner-dynamic"),
    SERVER("server-dynamic"),
    DEMO("demo-dynamic"),
}

expect class DynamicFeatureLoader {
    fun isInstalled(module: DynamicModule): Boolean
    suspend fun requestInstall(module: DynamicModule, onProgress: (Float) -> Unit = {}): Boolean

    /**
     * Asks the platform to remove an installed module. Play defers uninstalls to an
     * idle moment rather than removing code from under a running process, so this
     * returning true means "accepted", not "already gone".
     */
    suspend fun requestUninstall(module: DynamicModule): Boolean
}
