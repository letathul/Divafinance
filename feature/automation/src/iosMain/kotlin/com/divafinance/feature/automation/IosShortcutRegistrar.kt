package com.divafinance.feature.automation

import platform.Foundation.NSUserActivity
import platform.Intents.INShortcut
import platform.Intents.INVoiceShortcutCenter
// suggestedInvocationPhrase lives on the Intents framework's NSUserActivity
// category, so cinterop exposes it as an extension rather than a member.
import platform.Intents.setSuggestedInvocationPhrase

/**
 * Siri suggestions. The activity type is the shortcut id, which is what `iOSApp.swift`
 * hands back through `continueUserActivity` when one is invoked.
 */
class IosShortcutRegistrar : ShortcutRegistrar {

    private val registered = mutableListOf<ShortcutInfo>()

    override fun registerShortcuts(shortcuts: List<ShortcutInfo>) {
        registered.clear()
        registered.addAll(shortcuts)

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

    override fun unregisterShortcut(id: String) {
        registered.removeAll { it.id == id }
        registerShortcuts(registered.toList())
    }

    override fun getRegisteredShortcuts(): List<ShortcutInfo> = registered.toList()

    override fun handleDeepLink(uri: String): ShortcutInfo? =
        registered.find { it.deepLinkUri == uri }
}
