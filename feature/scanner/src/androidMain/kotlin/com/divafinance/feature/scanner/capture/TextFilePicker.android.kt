package com.divafinance.feature.scanner.capture

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `OpenDocument` rather than `GetContent`: it returns a stable, re-readable URI from the
 * system file picker, which is where a bank's exported statement actually lives.
 */
@Composable
actual fun rememberTextFilePicker(): TextFilePicker {
    val context = LocalContext.current

    // Held in a holder for the same reason the image launchers do it: the launcher's
    // callback is fixed at creation, while each request reports to a different caller.
    val pending = remember { arrayOfNulls<(TextFileResult) -> Unit>(1) }

    val open = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val callback = pending[0]
        pending[0] = null
        if (uri == null) {
            callback?.invoke(TextFileResult.Cancelled)
            return@rememberLauncherForActivityResult
        }
        val result = runCatching {
            val content = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().decodeToString() }
                ?: error("Couldn't open that file")
            TextFileResult.Success(context.displayNameOf(uri), content)
        }
        callback?.invoke(
            result.getOrElse { TextFileResult.Failed(it.message ?: "Couldn't read that file") },
        )
    }

    return remember(open) {
        TextFilePicker { onResult ->
            pending[0] = onResult
            // Banks label CSV exports inconsistently — some as text/csv, some as
            // application/vnd.ms-excel, some as octet-stream — so the filter is broad and
            // the parser is what actually rejects a file that isn't a statement.
            open.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "*/*"))
        }
    }
}

private fun android.content.Context.displayNameOf(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) return cursor.getString(column)
        }
    return uri.lastPathSegment ?: "statement.csv"
}
