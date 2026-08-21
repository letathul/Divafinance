package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a downsampled preview of a captured receipt, or null where the platform has no
 * decoder available. Callers render a neutral placeholder for null rather than branching on
 * the target.
 *
 * Compose Multiplatform has no common file→[ImageBitmap] loader and the project carries no
 * image-loading library, so this is Android-only for now.
 */
@Composable
expect fun rememberReceiptThumbnail(path: String?, maxDimension: Int = 512): ImageBitmap?
