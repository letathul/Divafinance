package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * No preview on iOS yet. `UIImage` → `CGImage` → pixel buffer → [ImageBitmap] is possible but
 * is a meaningful chunk of interop for a thumbnail, and the review screen's placeholder covers
 * the gap. The scan itself works fully — only the preview is missing.
 */
@Composable
actual fun rememberReceiptThumbnail(path: String?, maxDimension: Int): ImageBitmap? = null
