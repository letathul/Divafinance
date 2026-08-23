package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Deliberately not a `JFileChooser`, for the same reason `rememberImageCaptureRequester`
 * isn't: a modal Swing dialog would hang the Compose UI tests this target exists to host.
 */
@Composable
actual fun rememberTextFilePicker(): TextFilePicker = remember {
    TextFilePicker { onResult ->
        onResult(TextFileResult.Failed("Choosing a file isn't available on this platform"))
    }
}
