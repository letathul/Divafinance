package com.divafinance.feature.scanner.capture

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberReceiptThumbnail(path: String?, maxDimension: Int): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path, maxDimension) {
        value = path?.let { withContext(Dispatchers.IO) { decodeDownsampled(it, maxDimension) } }
    }
    return state.value
}

/**
 * Decoding is done in two passes because the first one is what keeps this from OOMing: a 12MP
 * phone photo at full size is ~48MB of ARGB_8888, and receipts are the largest images this app
 * ever touches.
 */
private fun decodeDownsampled(path: String, maxDimension: Int): ImageBitmap? {
    if (!File(path).exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        bounds.outWidth / (sampleSize * 2) >= maxDimension &&
        bounds.outHeight / (sampleSize * 2) >= maxDimension
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return runCatching { BitmapFactory.decodeFile(path, options)?.asImageBitmap() }.getOrNull()
}
