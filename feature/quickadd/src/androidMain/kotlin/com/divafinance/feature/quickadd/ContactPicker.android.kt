package com.divafinance.feature.quickadd

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * `READ_CONTACTS` plus a query, rather than the system single-contact picker.
 *
 * The picker needs no permission, but it returns exactly one contact per launch — the sheet
 * offers a searchable list to tick several people off, which is a whole address book or
 * nothing. The permission is asked for at the moment the row is tapped, never on launch.
 */
@Composable
actual fun rememberContactImporter(): ContactImporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The launcher's callback is fixed at creation while each request reports to a
    // different caller, so the pending one is held beside it.
    val pending = remember { arrayOfNulls<(List<String>) -> Unit>(1) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val onResult = pending[0]
        pending[0] = null
        if (onResult == null) return@rememberLauncherForActivityResult
        if (granted) {
            scope.readContacts(context, onResult)
        } else {
            onResult(emptyList())
        }
    }

    return remember(launcher, context, scope) {
        ContactImporter { onResult ->
            // Context.checkSelfPermission rather than ContextCompat: it exists from API
            // 23 and minSdk is 26, so the compat shim would only add a dependency.
            val held = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

            if (held) {
                scope.readContacts(context, onResult)
            } else {
                pending[0] = onResult
                launcher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }
}

actual fun contactsSupported(): Boolean = true

/** Off the main thread: a large address book is a cursor walk, not a lookup. */
private fun CoroutineScope.readContacts(context: Context, onResult: (List<String>) -> Unit) {
    launch {
        val names = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.displayNames() }.getOrDefault(emptyList())
        }
        onResult(names)
    }
}

private fun ContentResolver.displayNames(): List<String> {
    val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
    return query(
        ContactsContract.Contacts.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
    )?.use { cursor ->
        val column = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        if (column < 0) return@use emptyList()
        buildList {
            while (cursor.moveToNext()) {
                cursor.getString(column)?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }.orEmpty().distinct()
}
