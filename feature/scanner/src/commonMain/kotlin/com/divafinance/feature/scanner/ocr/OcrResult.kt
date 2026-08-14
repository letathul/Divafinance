package com.divafinance.feature.scanner.ocr

data class OcrResult(
    val fullText: String,
    val lines: List<String>,
)
