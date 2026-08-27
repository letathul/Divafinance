package com.divafinance.feature.quickadd

import androidx.compose.runtime.Composable

/**
 * Reads display names out of the device's address book.
 *
 * Names only — no numbers, no emails, no ids. A person on a split bill is a name and a
 * colour in this app's own database, so anything more would be data collected for no
 * record that could hold it. Nothing read here leaves the device.
 *
 * This lives in the feature rather than `core:common` for the same reason
 * [LocationPermissionRequester] does: prompting needs an Activity result launcher, which
 * means Compose and `androidx.activity`, and `core:common` is a non-Compose module that
 * every other module depends on.
 */
fun interface ContactImporter {
    /**
     * Prompts if needed, then reports the names found.
     *
     * [onResult] gets an empty list when permission is refused or there is nothing to read.
     * A refusal is not an error: the sheet's own add-by-name path covers the same ground.
     */
    fun import(onResult: (names: List<String>) -> Unit)
}

/** An importer bound to the current screen. */
@Composable
expect fun rememberContactImporter(): ContactImporter

/**
 * Whether this platform has an address book to read at all.
 *
 * Read once when the sheet opens, so the Import row is never offered on a target that
 * cannot honour it — an affordance that always fails is worse than no affordance.
 */
expect fun contactsSupported(): Boolean
