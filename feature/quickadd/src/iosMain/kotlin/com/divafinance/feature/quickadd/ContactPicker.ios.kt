package com.divafinance.feature.quickadd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Contacts.CNContact
import platform.Contacts.CNContactFamilyNameKey
import platform.Contacts.CNContactFetchRequest
import platform.Contacts.CNContactGivenNameKey
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * `CNContactStore` asks for access itself, so there is no launcher here — the store is
 * created, access is requested, and the answer arrives on a completion block.
 *
 * Requires `NSContactsUsageDescription` in the app's Info.plist. Without it iOS denies the
 * request outright rather than prompting, which surfaces here as an empty list and leaves
 * the sheet's add-by-name path as the way through.
 */
@Composable
actual fun rememberContactImporter(): ContactImporter {
    // Held across recompositions so a request in flight is not abandoned by a redraw.
    val store = remember { CNContactStore() }

    return remember(store) {
        ContactImporter { onResult -> store.importNames(onResult) }
    }
}

actual fun contactsSupported(): Boolean = true

private fun CNContactStore.importNames(onResult: (List<String>) -> Unit) {
    requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
        // Both the access callback and the enumeration run off the main queue; the sheet
        // is Compose state, so the answer is handed back on the main one.
        val names = if (granted) runCatching { fetchNames() }.getOrDefault(emptyList()) else emptyList()
        dispatch_async(dispatch_get_main_queue()) { onResult(names) }
    }
}

// The enumeration block is handed a raw `stop` pointer, which is cinterop's territory.
@OptIn(ExperimentalForeignApi::class)
private fun CNContactStore.fetchNames(): List<String> {
    val request = CNContactFetchRequest(
        keysToFetch = listOf(CNContactGivenNameKey, CNContactFamilyNameKey),
    )
    val names = mutableListOf<String>()
    enumerateContactsWithFetchRequest(request, null) { contact, _ ->
        (contact as? CNContact)?.let { person ->
            val name = "${person.givenName} ${person.familyName}".trim()
            if (name.isNotEmpty()) names += name
        }
    }
    return names.distinct().sortedBy { it.lowercase() }
}
