package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable

sealed interface TextFileResult {
    /** The file's whole contents. The name is for showing the user what they picked. */
    data class Success(val fileName: String, val content: String) : TextFileResult

    /** The user backed out. Not an error — the form stays exactly as it was. */
    data object Cancelled : TextFileResult

    data class Failed(val message: String) : TextFileResult
}

/**
 * Picks a text file and reads it, for the CSV statement import.
 *
 * Returns the *content* rather than a path, unlike [ImageCaptureRequester]. A statement is
 * read once and never referred to again — nothing persists a reference to it — so copying
 * the file into app storage the way a receipt image is copied would leave a file behind
 * that nothing ever reads.
 *
 * Lives in the feature rather than `core:common` for the same reason
 * [ImageCaptureRequester] does: it needs an Activity result launcher.
 */
fun interface TextFilePicker {
    fun pick(onResult: (TextFileResult) -> Unit)
}

/**
 * A picker bound to the current screen. Where no document picker exists this reports
 * [TextFileResult.Failed] rather than pretending to prompt — the paste box beside it is
 * still a working way in.
 */
@Composable
expect fun rememberTextFilePicker(): TextFilePicker
