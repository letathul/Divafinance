package com.divafinance.feature.scanner.ocr

import com.divafinance.core.domain.usecase.scanner.OcrLine

/**
 * [lines] carry per-line geometry so `ReceiptParser` can rebuild the receipt's printed rows;
 * [fullText] is the same content in the engine's own order, kept because it is what gets stored
 * in `Receipt.ocr_text` and shown when a scan needs to be re-read by hand.
 */
data class OcrResult(
    val fullText: String,
    val lines: List<OcrLine>,
)
