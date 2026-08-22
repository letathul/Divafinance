package com.divafinance.core.domain.usecase.scanner

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Subtotal, tax, tip, currency and line items — everything [ParsedReceipt] gained beyond the
 * original merchant/total/date triple. The totals cases share their vocabulary with
 * `ReceiptParserTest`'s exclusion cases by design: the same words that keep a figure out of the
 * total are what identify it here.
 */
class ReceiptParserDetailTest {

    private val today = LocalDate(2024, 3, 15)

    private fun parse(text: String) = ReceiptParser.parse(text, today)

    private val diner = """
        Bella Cucina
        Margherita Pizza 12.50
        2 x Espresso 7.00
        Tiramisu 6.50
        Subtotal 26.00
        Tax 2.08
        Tip 5.00
        Total 33.08
    """.trimIndent()

    // ── Subtotal / tax / tip ─────────────────────────────────────────────────────────

    @Test
    fun readsSubtotalTaxAndTipAlongsideTheTotal() {
        val parsed = parse(diner)

        assertEquals(26.00, parsed.subtotal)
        assertEquals(2.08, parsed.tax)
        assertEquals(5.00, parsed.tip)
        assertEquals(33.08, parsed.totalAmount)
    }

    @Test
    fun readsGratuityAsTip() {
        val text = """
            Bella Cucina
            Total 33.08
            Gratuity 6.00
        """.trimIndent()

        assertEquals(6.00, parse(text).tip)
    }

    @Test
    fun readsVatAsTax() {
        val text = """
            Corner Shop
            VAT 20% 4.00
            Total 24.00
        """.trimIndent()

        assertEquals(4.00, parse(text).tax)
    }

    @Test
    fun leavesAbsentAmountsNull() {
        val text = """
            Corner Shop
            Total 24.00
        """.trimIndent()

        val parsed = parse(text)

        // Null, not 0.0 — "no tip line was printed" is not "the tip was nothing".
        assertNull(parsed.tip)
        assertNull(parsed.tax)
        assertNull(parsed.subtotal)
    }

    @Test
    fun readsAnAmountPrintedOnTheLineBelowItsLabel() {
        val text = """
            Corner Shop
            Tax
            3.36
            Total 45.36
        """.trimIndent()

        assertEquals(3.36, parse(text).tax)
    }

    // ── Currency ─────────────────────────────────────────────────────────────────────

    @Test
    fun readsAnExplicitCurrencyCode() {
        val text = """
            Kaffeehaus
            Total EUR 12.50
        """.trimIndent()

        assertEquals("EUR", parse(text).currency)
    }

    @Test
    fun fallsBackToTheSymbol() {
        val text = """
            Corner Shop
            Total £24.00
        """.trimIndent()

        assertEquals("GBP", parse(text).currency)
    }

    @Test
    fun prefersAnExplicitCodeOverAnAmbiguousSymbol() {
        // "$" is shared by several currencies, so a printed code has to win.
        val text = """
            Maple Diner
            Total $24.00
            Amount charged in CAD
        """.trimIndent()

        assertEquals("CAD", parse(text).currency)
    }

    @Test
    fun leavesCurrencyNullWhenTheReceiptDoesNotSay() {
        val text = """
            Corner Shop
            Total 24.00
        """.trimIndent()

        assertNull(parse(text).currency)
    }

    // ── Line items ───────────────────────────────────────────────────────────────────

    @Test
    fun readsItemsFromTheBodyOfTheReceipt() {
        val items = parse(diner).items

        assertEquals(3, items.size)
        assertEquals("Margherita Pizza", items[0].description)
        assertEquals(12.50, items[0].price)
        assertEquals("Tiramisu", items[2].description)
        assertEquals(6.50, items[2].price)
    }

    @Test
    fun readsAPrintedQuantity() {
        val espresso = parse(diner).items[1]

        assertEquals("Espresso", espresso.description)
        assertEquals(2.0, espresso.quantity)
        // The price on the line is what the line was charged, not the price of one.
        assertEquals(7.00, espresso.price)
    }

    @Test
    fun stopsAtTheTotalsBlock() {
        // Subtotal/Tax/Tip/Total all carry a figure and would otherwise read as purchases.
        val descriptions = parse(diner).items.map { it.description }

        assertTrue(descriptions.none { it.contains("Subtotal", ignoreCase = true) })
        assertTrue(descriptions.none { it.contains("Tax", ignoreCase = true) })
        assertTrue(descriptions.none { it.contains("Total", ignoreCase = true) })
    }

    @Test
    fun ignoresTheMerchantHeaderAndAddress() {
        val text = """
            GreenLeaf Market
            123 Main Street
            Tel: 555-0134
            Bananas 3.20
            Total 3.20
        """.trimIndent()

        assertEquals(listOf("Bananas"), parse(text).items.map { it.description })
    }

    @Test
    fun ignoresADatedHeaderLineCarryingAFigure() {
        val text = """
            GreenLeaf Market
            03/11/2024 14.99
            Bananas 3.20
            Total 3.20
        """.trimIndent()

        assertEquals(listOf("Bananas"), parse(text).items.map { it.description })
    }

    @Test
    fun requiresCentsSoALoyaltyNumberIsNotAPrice() {
        val text = """
            GreenLeaf Market
            Member 4029301
            Bananas 3.20
            Total 3.20
        """.trimIndent()

        assertEquals(listOf("Bananas"), parse(text).items.map { it.description })
    }

    @Test
    fun returnsNoItemsWhenTheReceiptIsNotItemised() {
        val text = """
            Corner Shop
            Total 24.00
        """.trimIndent()

        assertEquals(emptyList(), parse(text).items)
    }

    @Test
    fun readsItemsAcrossColumnsWhenGeometryIsAvailable() {
        // The two-column form: descriptions in one block, prices in another.
        fun line(text: String, row: Int, from: Float, to: Float) = OcrLine(
            text = text,
            left = from,
            top = row * 0.05f,
            right = to,
            bottom = row * 0.05f + 0.04f,
        )
        val lines = listOf(
            line("Bella Cucina", row = 0, from = 0.1f, to = 0.6f),
            line("Margherita Pizza", row = 2, from = 0.1f, to = 0.5f),
            line("Tiramisu", row = 3, from = 0.1f, to = 0.3f),
            line("Total", row = 5, from = 0.1f, to = 0.25f),
            line("12.50", row = 2, from = 0.78f, to = 0.9f),
            line("6.50", row = 3, from = 0.78f, to = 0.9f),
            line("19.00", row = 5, from = 0.78f, to = 0.9f),
        )

        val parsed = ReceiptParser.parse(lines, today)

        assertEquals(listOf("Margherita Pizza", "Tiramisu"), parsed.items.map { it.description })
        assertEquals(listOf(12.50, 6.50), parsed.items.map { it.price })
        assertEquals(19.00, parsed.totalAmount)
    }
}
