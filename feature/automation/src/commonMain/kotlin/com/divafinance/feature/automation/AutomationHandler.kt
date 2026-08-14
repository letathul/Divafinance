package com.divafinance.feature.automation

data class ShortcutInfo(
    val id: String,
    val title: String,
    val description: String,
    val deepLinkUri: String,
)

expect class AutomationHandler() {
    fun registerShortcuts(shortcuts: List<ShortcutInfo>)
    fun unregisterShortcut(id: String)
    fun getRegisteredShortcuts(): List<ShortcutInfo>
    fun handleDeepLink(uri: String): ShortcutInfo?
}
