package com.divafinance.feature.automation

import platform.Foundation.NSUserActivity
import platform.Intents.INShortcut
import platform.Intents.INVoiceShortcutCenter

actual class AutomationHandler actual constructor() {

    private val registeredShortcuts = mutableListOf<ShortcutInfo>()

    actual fun registerShortcuts(shortcuts: List<ShortcutInfo>) {
        registeredShortcuts.clear()
        registeredShortcuts.addAll(shortcuts)

        val siriShortcuts = shortcuts.map { shortcut ->
            val activity = NSUserActivity(shortcut.id).apply {
                setTitle(shortcut.title)
                setEligibleForSearch(true)
                setEligibleForPrediction(true)
                setSuggestedInvocationPhrase(shortcut.title)
            }
            INShortcut(userActivity = activity)
        }

        INVoiceShortcutCenter.sharedCenter().setShortcutSuggestions(siriShortcuts)
    }

    actual fun unregisterShortcut(id: String) {
        registeredShortcuts.removeAll { it.id == id }
        registerShortcuts(registeredShortcuts.toList())
    }

    actual fun getRegisteredShortcuts(): List<ShortcutInfo> =
        registeredShortcuts.toList()

    actual fun handleDeepLink(uri: String): ShortcutInfo? =
        registeredShortcuts.find { it.deepLinkUri == uri }
}
