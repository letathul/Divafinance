package com.divafinance.feature.scanner.ocr

/**
 * No OCR on the desktop JVM target — it exists to host Compose tests, not to scan receipts.
 * [isAvailable] returns false so callers take the same "scanner unavailable" path they take
 * on a device where the on-demand scanner module is not installed.
 */
actual class OcrEngine actual constructor() {

    actual suspend fun recognizeText(imagePath: String): OcrResult =
        OcrResult(fullText = "", lines = emptyList())

    actual fun isAvailable(): Boolean = false
}
