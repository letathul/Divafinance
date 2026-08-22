package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable

/**
 * Where the receipt image comes from. Separate entry points, not one combined picker sheet.
 *
 * [DOCUMENT_SCAN] is the preferred one where the OS provides it: it finds the receipt's edges,
 * fires the shutter itself when the page is square in frame, corrects the perspective, and lets
 * the user crop or retake before handing anything back — all of which a plain [CAMERA] photo
 * leaves to the OCR pass to cope with. It is also the only source that can return several pages.
 */
enum class ImageSource { DOCUMENT_SCAN, CAMERA, PHOTO_LIBRARY }

sealed interface ImageCaptureResult {
    /**
     * One or more readable paths in the app's own storage, in page order. Paths rather than URIs
     * because both OCR engines open one directly, and because `Receipt.imagePath` is persisted
     * and re-read by the scan history long after any content-provider grant has expired.
     */
    data class Success(val paths: List<String>) : ImageCaptureResult {
        constructor(path: String) : this(listOf(path))

        init {
            require(paths.isNotEmpty()) { "A successful capture has at least one page" }
        }

        /** The first page — what every single-page caller means. */
        val path: String get() = paths.first()
    }

    /** The user backed out. Not an error — the screen returns to where it was, silently. */
    data object Cancelled : ImageCaptureResult

    /** No camera, permission refused, or the image couldn't be read. */
    data class Failed(val message: String) : ImageCaptureResult
}

/**
 * Acquires a receipt image and hands back local file paths.
 *
 * This lives in the feature rather than `core:common` for the same reason
 * `LocationPermissionRequester` does: it needs an Activity result launcher, which means
 * Compose and `androidx.activity`, and `core:common` is a non-Compose module that every other
 * module depends on.
 */
fun interface ImageCaptureRequester {
    fun request(source: ImageSource, onResult: (ImageCaptureResult) -> Unit)
}

/**
 * A requester bound to the current screen. On platforms with no camera or picker this reports
 * [ImageCaptureResult.Failed] rather than pretending to prompt.
 */
@Composable
expect fun rememberImageCaptureRequester(): ImageCaptureRequester

/**
 * Whether [ImageSource.DOCUMENT_SCAN] will actually open a scanner on this device.
 *
 * Worth asking rather than assuming: on Android the scanner UI is delivered by Play services
 * and can be absent, and on iOS `VNDocumentCameraViewController` needs a camera the simulator
 * does not have. Callers fall back to [ImageSource.CAMERA], which works everywhere.
 */
@Composable
expect fun isDocumentScanSupported(): Boolean
