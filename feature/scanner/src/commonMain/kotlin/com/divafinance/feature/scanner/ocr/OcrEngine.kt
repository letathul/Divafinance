package com.divafinance.feature.scanner.ocr

expect class OcrEngine() {
    suspend fun recognizeText(imagePath: String): OcrResult
    fun isAvailable(): Boolean
}
