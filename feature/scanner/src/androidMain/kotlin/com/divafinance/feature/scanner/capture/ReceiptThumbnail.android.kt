package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberReceiptThumbnail(path: String?, maxDimension: Int): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path, maxDimension) {
        value = path?.let {
            withContext(Dispatchers.IO) { decodeOriented(it, maxDimension)?.asImageBitmap() }
        }
    }
    return state.value
}
