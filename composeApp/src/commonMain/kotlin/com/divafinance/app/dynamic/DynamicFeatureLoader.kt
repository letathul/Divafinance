package com.divafinance.app.dynamic

enum class DynamicModule(val moduleName: String) {
    // Play split names come from the Gradle project name, so these must stay in
    // lockstep with the `:dynamic:*` module directories (underscores, not hyphens) —
    // a mismatch makes isInstalled() permanently false and installs fail outright.
    MAP("map_dynamic"),
    SCANNER("scanner_dynamic"),
    SERVER("server_dynamic"),
    DEMO("demo_dynamic"),
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
