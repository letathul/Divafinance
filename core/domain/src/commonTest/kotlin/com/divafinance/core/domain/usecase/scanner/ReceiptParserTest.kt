package com.divafinance.core.domain.usecase.scanner

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Each case here is a shape real OCR output takes. The total cases in particular are the
 * regression suite for the old single-regex implementation, which took the first `total` match
 * in the whole text and so returned the subtotal on the most common receipt layout there is.
 */
class ReceiptParserTest {

    private val today = LocalDate(2024, 3, 15)

    private fun total(text: String) = ReceiptParser.parse(text, today).totalAmount
    private fun merchant(text: String) = ReceiptParser.parse(text, today).merchantName
    private fun date(text: String, dayFirst: Boolean = false) =
        ReceiptParser.parse(text, today, dayFirst).date

    // ── Total ────────────────────────────────────────────────────────────────────────

    @Test
    fun takesTotalNotSubtotal() {
        val text = """
            GreenLeaf Market
            Subtotal 42.00
            Tax 3.36
            Total 45.36
        """.trimIndent()

        assertEquals(45.36, total(text))
    }

    @Test
    fun ignoresTotalSavingsLine() {
        val text = """
            SuperMart
            Total Savings 3.00
            Total 21.499
            Total 18.75
        """.trimIndent()

        assertEquals(18.75, total(text))
    }

    @Test
    fun ignoresTaxLine() {
        assertEquals(12.00, total("Shop\nTax 0.96\nTotal 12.00"))
    }

    @Test
    fun ignoresChangeDueLine() {
        assertEquals(45.36, total("Shop\nTotal 45.36\nCash 50.00\nChange Due 4.64"))
    }

    @Test
    fun ignoresTotalItemCount() {
        assertEquals(31.20, total("Shop\nTotal Items 4\nTotal 31.20"))
    }

    @Test
    fun parsesThousandsSeparator() {
        assertEquals(1234.56, total("Store\nTotal: $1,234.56"))
    }

    @Test
    fun parsesEuropeanDecimalComma() {
        assertEquals(12.50, total("Bäckerei\nTotal 12,50"))
    }

    @Test
    fun grandTotalOutranksAStrayTotal() {
        val text = """
            Hotel Bill
            Total room charge 100.00
            Grand Total 250.00
        """.trimIndent()

        assertEquals(250.00, total(text))
    }

    @Test
    fun recognisesAmountDue() {
        assertEquals(88.20, total("Utility Co\nAmount Due 88.20"))
    }

    @Test
    fun readsValueFromTheNextLineInTwoColumnLayouts() {
        assertEquals(45.36, total("Shop\nTOTAL\n45.36"))
    }

    @Test
    fun lastTotalWinsWhenSeveralAppear() {
        assertEquals(20.00, total("Shop\nTotal 10.00\nAdjustment\nTotal 20.00"))
    }

    @Test
    fun toleratesSpaceAfterCurrencySymbol() {
        assertEquals(45.36, total("Shop\nTotal: $ 45.36"))
    }

    @Test
    fun fallsBackToAWholeNumberTotal() {
        assertEquals(45.0, total("Shop\nTotal 45"))
    }

    @Test
    fun returnsNullWhenThereIsNoTotal() {
        assertNull(total("Some Store\nItems purchased\nThank you"))
    }

    @Test
    fun doesNotMistakeAPhoneNumberForATotal() {
        assertNull(total("Some Store\nTel: 555 123 4567\nThank you"))
    }

    // ── Merchant ─────────────────────────────────────────────────────────────────────

    @Test
    fun takesThePlainFirstLine() {
        assertEquals("Starbucks Coffee", merchant("Starbucks Coffee\nLatte 5.50\nTotal 5.50"))
    }

    @Test
    fun skipsAGreeting() {
        assertEquals("Trader Joe's", merchant("WELCOME TO\nTrader Joe's\nTotal 30.00"))
    }

    @Test
    fun skipsAPhoneNumber() {
        assertEquals("Corner Deli", merchant("(555) 123-4567\nCorner Deli\nTotal 9.00"))
    }

    @Test
    fun skipsAStreetAddress() {
        assertEquals("Blue Bottle", merchant("123 Main Street\nBlue Bottle\nTotal 6.00"))
    }

    @Test
    fun skipsTheWordReceipt() {
        assertEquals("Home Depot", merchant("RECEIPT\nHome Depot\nTotal 75.00"))
    }

    @Test
    fun fallsBackToTheFirstLineWhenEverythingIsFiltered() {
        // Better a noisy merchant than none — the review screen lets the user fix it.
        assertEquals("RECEIPT", merchant("RECEIPT"))
    }

    @Test
    fun collapsesInternalWhitespace() {
        assertEquals("Whole Foods", merchant("Whole   Foods\nTotal 40.00"))
    }

    @Test
    fun preservesCasingOfAcronyms() {
        // Title-casing would turn this into "Ikea".
        assertEquals("IKEA", merchant("IKEA\nBILLY bookcase\nTotal 79.00"))
    }

    @Test
    fun returnsNullForEmptyText() {
        assertNull(merchant(""))
    }

    // ── Date ─────────────────────────────────────────────────────────────────────────

    @Test
    fun parsesIsoDate() {
        assertEquals(LocalDate(2024, 3, 4), date("Shop\n2024-03-04\nTotal 10.00"))
    }

    @Test
    fun defaultsAmbiguousNumericDateToMonthFirst() {
        assertEquals(LocalDate(2024, 3, 4), date("Shop\n03/04/2024\nTotal 10.00"))
    }

    @Test
    fun honoursDayFirstWhenRequested() {
        // Month-first would read this as October 3rd — in the future, so the sanity window
        // would drop it. Day-first gives March 10th.
        assertEquals(LocalDate(2024, 3, 10), date("Shop\n10/03/2024\nTotal 10.00", dayFirst = true))
    }

    @Test
    fun disambiguatesByDayGreaterThanTwelve() {
        assertEquals(LocalDate(2023, 12, 25), date("Shop\n25/12/2023\nTotal 10.00"))
    }

    @Test
    fun parsesMonthFirstTextualDate() {
        assertEquals(LocalDate(2024, 3, 4), date("Shop\nMar 4, 2024\nTotal 10.00"))
    }

    @Test
    fun parsesDayFirstTextualDate() {
        assertEquals(LocalDate(2024, 3, 4), date("Shop\n4 March 2024\nTotal 10.00"))
    }

    @Test
    fun expandsTwoDigitYear() {
        assertEquals(LocalDate(2024, 3, 4), date("Shop\n03/04/24\nTotal 10.00"))
    }

    @Test
    fun rejectsAnImpossibleCalendarDate() {
        assertNull(date("Shop\n02/31/2024\nTotal 10.00"))
    }

    @Test
    fun rejectsADateFarInTheFuture() {
        // A misread loyalty number, not a purchase date.
        assertNull(date("Shop\n2087-01-01\nTotal 10.00"))
    }

    @Test
    fun rejectsADateFarInThePast() {
        assertNull(date("Shop\n1999-01-01\nTotal 10.00"))
    }

    @Test
    fun headerDateBeatsAFooterCardExpiry() {
        val text = """
            Shop
            2024-03-04
            VISA ending 1234 exp 12/28
            Total 10.00
        """.trimIndent()

        assertEquals(LocalDate(2024, 3, 4), date(text))
    }

    @Test
    fun returnsNullWhenThereIsNoDate() {
        assertNull(date("Shop\nLatte 5.50\nTotal 5.50"))
    }
}
