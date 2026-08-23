package com.divafinance.feature.automation

data class ShortcutInfo(
    val id: String,
    val title: String,
    val description: String,
    val deepLinkUri: String,
)

/**
 * Publishes the enabled automations to whatever the platform calls a shortcut — dynamic
 * app shortcuts on Android, Siri suggestions on iOS.
 *
 * An interface rather than the `expect class AutomationHandler()` this used to be. The
 * Android implementation needs a `Context`, which a no-argument `expect` constructor
 * cannot carry, and common code no longer constructs one: `platformModule()` binds the
 * right implementation per platform, exactly as it does for `FileSystem`.
 */
interface ShortcutRegistrar {
    /** Replaces the whole set — [AutomationViewModel] relies on that, so no partial update. */
    fun registerShortcuts(shortcuts: List<ShortcutInfo>)
    fun unregisterShortcut(id: String)
    fun getRegisteredShortcuts(): List<ShortcutInfo>
    fun handleDeepLink(uri: String): ShortcutInfo?
}

/**
 * Records shortcuts without publishing them. The desktop/test host has no shortcut
 * surface to publish to, and this keeps the ViewModel's behaviour assertable without a
 * device.
 */
class InMemoryShortcutRegistrar : ShortcutRegistrar {

    private val registered = mutableListOf<ShortcutInfo>()

    override fun registerShortcuts(shortcuts: List<ShortcutInfo>) {
        registered.clear()
        registered.addAll(shortcuts)
    }

    override fun unregisterShortcut(id: String) {
        registered.removeAll { it.id == id }
    }

    override fun getRegisteredShortcuts(): List<ShortcutInfo> = registered.toList()

    override fun handleDeepLink(uri: String): ShortcutInfo? =
        registered.find { it.deepLinkUri == uri }
}
