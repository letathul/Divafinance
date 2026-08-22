package com.divafinance.core.domain.usecase.scanner

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The geometry overload of [ReceiptParser.parse].
 *
 * Every case here is built from hand-written coordinates rather than a real image: the point
 * under test is the regrouping of positioned lines into printed rows, and an OCR engine is not
 * needed to exercise it. Coordinates are fractions of the page with a top-left origin, matching
 * [OcrLine]'s contract.
 */
class ReceiptParserLayoutTest {

    private val today = LocalDate(2024, 3, 15)

    /** One line on a notional 20-row page, occupying the horizontal span [from, to]. */
    private fun line(text: String, row: Int, from: Float, to: Float) = OcrLine(
        text = text,
        left = from,
        top = row * 0.05f,
        right = to,
        bottom = row * 0.05f + 0.04f,
    )

    @Test
    fun pairsALabelWithTheAmountInItsRightColumn() {
        // The shape ML Kit actually produces: the whole label column arrives as one block and
        // the whole amount column as another, so in engine order "Total" is nowhere near 45.36.
        val lines = listOf(
            line("GreenLeaf Market", row = 0, from = 0.1f, to = 0.6f),
            line("Subtotal", row = 4, from = 0.1f, to = 0.3f),
            line("Tax", row = 5, from = 0.1f, to = 0.2f),
            line("Total", row = 6, from = 0.1f, to = 0.25f),
            line("42.00", row = 4, from = 0.75f, to = 0.9f),
            line("3.36", row = 5, from = 0.78f, to = 0.9f),
            line("45.36", row = 6, from = 0.75f, to = 0.9f),
        )

        assertEquals(45.36, ReceiptParser.parse(lines, today).totalAmount)
    }

    @Test
    fun readsMerchantFromTheTopRowNotTheFirstBlock() {
        // The amount column sorts first in engine order, so without regrouping the merchant
        // would be read from a block that starts halfway down the page.
        val lines = listOf(
            line("12.00", row = 3, from = 0.8f, to = 0.9f),
            line("Total", row = 4, from = 0.1f, to = 0.25f),
            line("12.00", row = 4, from = 0.8f, to = 0.9f),
            line("Cafe Verona", row = 0, from = 0.1f, to = 0.5f),
        )

        assertEquals("Cafe Verona", ReceiptParser.parse(lines, today).merchantName)
    }

    @Test
    fun keepsLabelAndFigureAsSeparateTokens() {
        // Joining a row without a separator would fuse "Total" and "45.36" into "Total45.36",
        // which the money regex would still match but the label regex would not.
        val lines = listOf(
            line("Corner Store", row = 0, from = 0.1f, to = 0.5f),
            line("Total", row = 5, from = 0.1f, to = 0.25f),
            line("45.36", row = 5, from = 0.75f, to = 0.9f),
        )

        assertEquals(45.36, ReceiptParser.parse(lines, today).totalAmount)
    }

    @Test
    fun slightlyMisalignedColumnsStillCountAsOneRow() {
        // Real bounding boxes are never exactly level; a few percent of a line's height apart
        // is the same printed row.
        val lines = listOf(
            OcrLine("Corner Store", left = 0.1f, top = 0.02f, right = 0.5f, bottom = 0.06f),
            OcrLine("Amount Due", left = 0.1f, top = 0.400f, right = 0.35f, bottom = 0.440f),
            OcrLine("19.99", left = 0.78f, top = 0.408f, right = 0.90f, bottom = 0.448f),
        )

        assertEquals(19.99, ReceiptParser.parse(lines, today).totalAmount)
    }

    @Test
    fun separateRowsAreNotMerged() {
        val lines = listOf(
            line("Corner Store", row = 0, from = 0.1f, to = 0.5f),
            line("Subtotal  42.00", row = 4, from = 0.1f, to = 0.9f),
            line("Total  45.36", row = 6, from = 0.1f, to = 0.9f),
        )

        assertEquals(45.36, ReceiptParser.parse(lines, today).totalAmount)
    }

    @Test
    fun linesWithoutGeometryKeepTheirGivenOrder() {
        // The plain-text overload builds exactly this, so it must behave like the old parser.
        val lines = listOf("GreenLeaf Market", "Subtotal 42.00", "Total 45.36").map { OcrLine(it) }

        val parsed = ReceiptParser.parse(lines, today)

        assertEquals(45.36, parsed.totalAmount)
        assertEquals("GreenLeaf Market", parsed.merchantName)
    }

    @Test
    fun aPartiallyPositionedSetFallsBackToGivenOrder() {
        // Mixing positioned and unpositioned lines would sort the unpositioned ones to the top
        // of the page; keeping the caller's order is the only safe reading.
        val lines = listOf(
            OcrLine("GreenLeaf Market"),
            line("Total  45.36", row = 6, from = 0.1f, to = 0.9f),
        )

        assertEquals("GreenLeaf Market", ReceiptParser.parse(lines, today).merchantName)
    }

    @Test
    fun readsTheDateFromTheTopRowAcrossColumns() {
        val lines = listOf(
            line("Cafe Verona", row = 0, from = 0.1f, to = 0.5f),
            line("14.50", row = 6, from = 0.75f, to = 0.9f),
            line("Date", row = 1, from = 0.1f, to = 0.2f),
            line("2024-03-11", row = 1, from = 0.6f, to = 0.9f),
            line("Total", row = 6, from = 0.1f, to = 0.25f),
        )

        assertEquals(LocalDate(2024, 3, 11), ReceiptParser.parse(lines, today).date)
    }
}
