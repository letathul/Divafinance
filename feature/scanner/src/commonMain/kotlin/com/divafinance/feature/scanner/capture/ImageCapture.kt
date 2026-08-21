package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable

/** Where the receipt image comes from. Two entry points, not one combined picker sheet. */
enum class ImageSource { CAMERA, PHOTO_LIBRARY }

sealed interface ImageCaptureResult {
    /**
     * A readable path in the app's own storage. A path rather than a URI because both OCR
     * engines open one directly, and because `Receipt.imagePath` is persisted and re-read by
     * the scan history long after any content-provider grant has expired.
     */
    data class Success(val path: String) : ImageCaptureResult

    /** The user backed out. Not an error — the screen returns to where it was, silently. */
    data object Cancelled : ImageCaptureResult

    /** No camera, permission refused, or the image couldn't be read. */
    data class Failed(val message: String) : ImageCaptureResult
}

/**
 * Acquires a receipt image and hands back a local file path.
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
