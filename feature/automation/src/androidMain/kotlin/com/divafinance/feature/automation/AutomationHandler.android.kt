package com.divafinance.feature.automation

actual class AutomationHandler actual constructor() {

    private val registeredShortcuts = mutableListOf<ShortcutInfo>()

    actual fun registerShortcuts(shortcuts: List<ShortcutInfo>) {
        registeredShortcuts.clear()
        registeredShortcuts.addAll(shortcuts)
    }

    actual fun unregisterShortcut(id: String) {
        registeredShortcuts.removeAll { it.id == id }
    }

    actual fun getRegisteredShortcuts(): List<ShortcutInfo> =
        registeredShortcuts.toList()

    actual fun handleDeepLink(uri: String): ShortcutInfo? =
        registeredShortcuts.find { it.deepLinkUri == uri }
}
