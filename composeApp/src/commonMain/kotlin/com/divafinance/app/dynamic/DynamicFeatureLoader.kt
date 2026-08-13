package com.divafinance.app.dynamic

enum class DynamicModule(val moduleName: String) {
    MAP("map-dynamic"),
    SCANNER("scanner-dynamic"),
    SERVER("server-dynamic"),
}

expect class DynamicFeatureLoader {
    fun isInstalled(module: DynamicModule): Boolean
    suspend fun requestInstall(module: DynamicModule, onProgress: (Float) -> Unit = {}): Boolean
}
