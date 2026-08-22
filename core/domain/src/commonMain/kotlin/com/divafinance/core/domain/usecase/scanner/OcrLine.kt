package com.divafinance.core.domain.usecase.scanner

/**
 * One line of recognised text and where it sat on the page.
 *
 * Coordinates are **fractions of the image, top-left origin** — each OCR engine normalises into
 * that before handing lines over, because ML Kit reports pixels from the top-left and Vision
 * reports fractions from the bottom-left.
 *
 * Geometry is optional: the zero rectangle means "unknown", which is what
 * [ReceiptParser.parse]'s plain-text overload produces. [ReceiptParser] degrades to its
 * line-order heuristics in that case, so a caller with no coordinates loses nothing.
 *
 * This lives in `:core:domain` rather than beside the engine in `:feature:scanner` because the
 * parser consumes it and features may not be depended on from below.
 */
data class OcrLine(
    val text: String,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    internal val hasGeometry: Boolean get() = right > left && bottom > top
    internal val verticalCentre: Float get() = (top + bottom) / 2f
    internal val height: Float get() = bottom - top
}
