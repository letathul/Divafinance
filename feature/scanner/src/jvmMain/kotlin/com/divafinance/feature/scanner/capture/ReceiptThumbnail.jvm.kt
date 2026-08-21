package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The desktop target hosts Compose tests rather than real scans, and nothing here can produce
 * an image to preview — [rememberImageCaptureRequester] reports failure on this platform. The
 * review screen renders its placeholder instead.
 */
@Composable
actual fun rememberReceiptThumbnail(path: String?, maxDimension: Int): ImageBitmap? = null
