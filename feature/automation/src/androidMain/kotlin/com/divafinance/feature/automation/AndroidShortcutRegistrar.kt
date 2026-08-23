package com.divafinance.feature.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat

/**
 * Real Android dynamic shortcuts — what a long-press on the launcher icon shows.
 *
 * Each shortcut carries an `ACTION_VIEW` intent for its `divafinance://` URI, which
 * `MainActivity`'s second intent-filter answers. Without that filter these would appear in
 * the launcher and then open nothing.
 */
class AndroidShortcutRegistrar(private val context: Context) : ShortcutRegistrar {

    private val registered = mutableListOf<ShortcutInfo>()

    override fun registerShortcuts(shortcuts: List<ShortcutInfo>) {
        registered.clear()
        registered.addAll(shortcuts)

        // The launcher caps how many it will show and silently drops the overflow, so
        // trimming here keeps what is registered equal to what is displayed.
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val compat = shortcuts.take(limit).map { shortcut ->
            ShortcutInfoCompat.Builder(context, shortcut.id)
                .setShortLabel(shortcut.title)
                .setLongLabel(shortcut.description)
                .setIntent(
                    Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.deepLinkUri))
                        .setPackage(context.packageName),
                )
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, compat)
    }

    override fun unregisterShortcut(id: String) {
        registered.removeAll { it.id == id }
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }

    override fun getRegisteredShortcuts(): List<ShortcutInfo> = registered.toList()

    override fun handleDeepLink(uri: String): ShortcutInfo? =
        registered.find { it.deepLinkUri == uri }
}
