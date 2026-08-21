package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The desktop target exists to host Compose tests, not to scan receipts — `OcrEngine` already
 * reports itself unavailable here, so an image would have nowhere to go. Reports a failure for
 * the same reason the location requester reports a denial rather than faking a prompt.
 *
 * Deliberately not a `JFileChooser`: a modal Swing dialog would hang the Compose UI tests.
 */
@Composable
actual fun rememberImageCaptureRequester(): ImageCaptureRequester = remember {
    ImageCaptureRequester { _, onResult ->
        onResult(ImageCaptureResult.Failed("Receipt scanning isn't available on this platform"))
    }
}
