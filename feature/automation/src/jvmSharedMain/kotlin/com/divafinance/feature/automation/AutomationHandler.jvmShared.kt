package com.divafinance.feature.automation

/**
 * In-memory only — nothing is registered with the OS. The Android side is still a
 * placeholder (a real implementation would use `ShortcutManagerCompat`), which is why it
 * is currently identical to the desktop one and shared here rather than duplicated. Move
 * this back to `androidMain` when Android grows a real shortcut implementation.
 */
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
