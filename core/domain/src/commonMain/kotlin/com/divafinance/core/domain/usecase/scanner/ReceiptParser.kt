package com.divafinance.core.domain.usecase.scanner

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** What [ReceiptParser] could recover from a block of OCR text. Every field is best-effort. */
data class ParsedReceipt(
    val merchantName: String?,
    val totalAmount: Double?,
    val date: LocalDate?,
    val subtotal: Double? = null,
    val tax: Double? = null,
    val tip: Double? = null,
    /** ISO code, resolved from a symbol where the receipt only printed one. */
    val currency: String? = null,
    val items: List<ParsedLineItem> = emptyList(),
)

/** One purchased item as printed. [price] is what the line was charged at, not a unit price. */
data class ParsedLineItem(
    val description: String,
    val price: Double?,
    val quantity: Double? = null,
)

/**
 * Pure extraction from OCR text — no repository, no clock, no coroutine.
 *
 * `today` is a parameter rather than a `Clock.System` read so the sanity window on dates is
 * deterministic under test. OCR output is noisy in specific, predictable ways, and most of the
 * rules below exist to reject one of them; the tests name each case.
 */
object ReceiptParser {

    fun parse(ocrText: String, today: LocalDate, dayFirst: Boolean = false): ParsedReceipt =
        parse(ocrText.lines().map { OcrLine(it) }, today, dayFirst)

    /**
     * The layout-aware entry point. Lines carrying geometry are regrouped into visual rows
     * first, which is what lets `TOTAL` in the left column and `45.99` in the right column be
     * read as one line by the rules below. Without geometry this collapses to the plain
     * line order and every rule behaves exactly as it did before.
     */
    fun parse(lines: List<OcrLine>, today: LocalDate, dayFirst: Boolean = false): ParsedReceipt {
        val rows = groupIntoRows(lines)
        return ParsedReceipt(
            merchantName = extractMerchant(rows),
            totalAmount = extractTotal(rows),
            date = extractDate(rows, today, dayFirst),
            subtotal = extractLabelled(rows, SUBTOTAL_LABEL),
            tax = extractLabelled(rows, TAX_LABEL),
            tip = extractLabelled(rows, TIP_LABEL),
            currency = extractCurrency(rows),
            items = extractItems(rows),
        )
    }

    // ── Layout ───────────────────────────────────────────────────────────────────────

    /**
     * Lines whose vertical centres sit within [ROW_TOLERANCE] of each other are one printed row,
     * joined left-to-right. This is the single highest-value use of geometry: OCR engines emit
     * text block by block, so a receipt's label column and amount column arrive as two separate
     * runs of lines and every rule that reads "the figure on the total line" would otherwise
     * never see one.
     *
     * All-or-nothing on geometry — a partially positioned set would interleave positioned and
     * unpositioned lines unpredictably, so it falls back to the given order instead.
     */
    private fun groupIntoRows(lines: List<OcrLine>): List<String> {
        val present = lines.filter { it.text.isNotBlank() }
        if (present.isEmpty() || present.any { !it.hasGeometry }) {
            return present.map { it.text.trim() }.filter { it.isNotBlank() }
        }

        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in present.sortedBy { it.verticalCentre }) {
            val current = rows.lastOrNull()
            val anchor = current?.first()
            val tolerance = ROW_TOLERANCE * maxOf(line.height, anchor?.height ?: 0f)
            if (anchor != null && line.verticalCentre - anchor.verticalCentre <= tolerance) {
                current.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }
        // Two spaces, so a label and its figure never fuse into one token for [MONEY].
        return rows
            .map { row -> row.sortedBy { it.left }.joinToString("  ") { it.text.trim() }.trim() }
            .filter { it.isNotBlank() }
    }

    /** Fraction of a line's own height that two lines' centres may differ by and still be one row. */
    private const val ROW_TOLERANCE = 0.6f

    // ── Total ────────────────────────────────────────────────────────────────────────

    /**
     * Lines a total may never be taken from. `subtotal` is the one that matters: the previous
     * implementation searched the whole text for `total` and took the *first* hit, so on the
     * near-universal "Subtotal / Tax / Total" layout it returned the subtotal.
     */
    private val EXCLUDED_LINE = Regex(
        """(?i)\b(sub[\s-]?total|total\s+(?:savings?|discounts?|items?|qty|quantity|units?|tax)|""" +
            """sales\s+tax|tax|change\s+due|change|cash\s+(?:back|tender)|tender(?:ed)?|tip|gratuity)\b""",
    )

    /** Strongest label first. The first tier that yields a figure wins outright. */
    private val TOTAL_LABELS = listOf(
        Regex("""(?i)\b(grand\s+total|amount\s+due|balance\s+due|total\s+due|order\s+total)\b"""),
        Regex("""(?i)\btotals?\b"""),
        Regex("""(?i)\b(amount|balance|due)\b"""),
    )

    /**
     * A money figure that prints its cents. Requiring the decimal pair is what keeps phone
     * numbers, item counts, dates and loyalty-card numbers out of the result, and accepting
     * `[.,]` covers European `12,50`. [MONEY_LOOSE] is the fallback for a bare "TOTAL 45".
     */
    private val MONEY = Regex("""(\d{1,3}(?:,\d{3})+|\d+)[.,](\d{2})(?!\d)""")
    private val MONEY_LOOSE = Regex("""(?:^|\s)(\d{1,3}(?:,\d{3})+|\d+)(?:\s|$)""")

    private fun extractTotal(lines: List<String>): Double? {
        val candidates = lines.withIndex().filter { !EXCLUDED_LINE.containsMatchIn(it.value) }

        for (label in TOTAL_LABELS) {
            // Totals sit at the foot of a receipt, and a header may repeat the word.
            for ((index, line) in candidates.reversed()) {
                val match = label.find(line) ?: continue
                val afterLabel = line.substring(match.range.last + 1)
                val amount = money(afterLabel)
                    ?: money(line)
                    // Two-column layouts print the label alone with the figure below it.
                    ?: lines.getOrNull(index + 1)?.let { money(it) }
                if (amount != null) return amount
            }
        }
        return null
    }

    private fun money(text: String): Double? {
        MONEY.findAll(text).lastOrNull()?.let { return moneyOf(it) }
        return MONEY_LOOSE.findAll(text).lastOrNull()
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun moneyOf(match: MatchResult): Double? {
        val whole = match.groupValues[1].replace(",", "")
        return "$whole.${match.groupValues[2]}".toDoubleOrNull()
    }

    // ── Subtotal, tax, tip ───────────────────────────────────────────────────────────

    /**
     * The same vocabulary [EXCLUDED_LINE] uses to keep these figures *out* of the total, read
     * here as positive labels. They are worth capturing in their own right: a total that does
     * not equal subtotal plus tax plus tip is the cheapest signal available that a scan went
     * wrong, and tip is the one figure a card statement will disagree with.
     */
    private val SUBTOTAL_LABEL = Regex("""(?i)\bsub[\s-]?total\b""")
    private val TAX_LABEL = Regex("""(?i)\b(sales\s+tax|vat|gst|hst|tax)\b""")
    private val TIP_LABEL = Regex("""(?i)\b(tip|gratuity|service\s+charge)\b""")

    /** Bottom-up like [extractTotal] — all three print in the footer block. */
    private fun extractLabelled(lines: List<String>, label: Regex): Double? {
        for ((index, line) in lines.withIndex().reversed()) {
            val match = label.find(line) ?: continue
            val amount = money(line.substring(match.range.last + 1))
                // Two-column layouts print the label alone with the figure below it.
                ?: lines.getOrNull(index + 1)?.let { money(it) }
            if (amount != null) return amount
        }
        return null
    }

    // ── Currency ─────────────────────────────────────────────────────────────────────

    private val CURRENCY_CODE = Regex(
        """(?i)\b(USD|EUR|GBP|INR|JPY|CAD|AUD|CHF|CNY|SEK|NOK|DKK|NZD|SGD|HKD|AED|ZAR|MXN|BRL|PLN)\b""",
    )

    /**
     * `$` is shared by several currencies and resolving it to USD is a guess — but it is the
     * guess the review screen already made when it fell back to the base currency, and here at
     * least an explicit code on the receipt overrides it.
     */
    private val CURRENCY_SYMBOLS = mapOf(
        '$' to "USD", '€' to "EUR", '£' to "GBP", '¥' to "JPY", '₹' to "INR",
        '₩' to "KRW", '₪' to "ILS", '₺' to "TRY", '₽' to "RUB",
    )

    private fun extractCurrency(lines: List<String>): String? {
        for (line in lines) {
            CURRENCY_CODE.find(line)?.let { return it.groupValues[1].uppercase() }
        }
        for (line in lines) {
            for (character in line) {
                CURRENCY_SYMBOLS[character]?.let { return it }
            }
        }
        return null
    }

    // ── Line items ───────────────────────────────────────────────────────────────────

    /**
     * Where the itemised body ends. Everything from the first of these labels down is the
     * totals block, and reading items out of it would list "Subtotal" as a purchase.
     */
    private val FOOTER_LABEL = Regex(
        """(?i)\b(sub[\s-]?total|totals?|amount\s+due|balance\s+due|grand\s+total|sales\s+tax|""" +
            """vat|gst|hst|tax|tip|gratuity|change|tender(?:ed)?|cash|visa|mastercard|""" +
            """amex|debit|credit\s+card|payment|auth\s*code)\b""",
    )

    /** Leading `2 x`, `2X`, `2 @` — the forms receipts print a count in. */
    private val ITEM_QUANTITY = Regex("""^(\d{1,3})\s*[xX*@]\s*""")

    private const val MAX_ITEM_DESCRIPTION = 60

    /**
     * An item is a line with a description on the left and a figure with cents on the right.
     * Requiring the cents is doing the same work it does in [MONEY]: it is what keeps loyalty
     * numbers, weights and item counts from being read as prices. No loose fallback here —
     * a wrongly invented item is worse than a missing one, because the user has to notice it
     * to delete it.
     */
    private fun extractItems(lines: List<String>): List<ParsedLineItem> {
        val footerStart = lines.indexOfFirst { FOOTER_LABEL.containsMatchIn(it) }
        val body = if (footerStart >= 0) lines.take(footerStart) else lines

        return body.mapNotNull { line ->
            val match = MONEY.findAll(line).lastOrNull() ?: return@mapNotNull null
            // A dated line with a figure on it is a header, not a purchase.
            if (ISO.containsMatchIn(line) || NUMERIC.containsMatchIn(line)) return@mapNotNull null

            val head = line.substring(0, match.range.first)
                .trim()
                .trimEnd('.', ',', ':', ';', '-', '@', '$', '€', '£', '¥', '₹')
                .trim()
            if (head.isBlank() || head.length > MAX_ITEM_DESCRIPTION) return@mapNotNull null
            if (NOT_A_MERCHANT.matches(head)) return@mapNotNull null
            if (LETTER.findAll(head).take(2).count() < 2) return@mapNotNull null

            val quantity = ITEM_QUANTITY.find(head)
            val description = quantity?.let { head.substring(it.range.last + 1) } ?: head
            if (description.isBlank()) return@mapNotNull null

            ParsedLineItem(
                description = clean(description),
                price = moneyOf(match),
                quantity = quantity?.groupValues?.get(1)?.toDoubleOrNull(),
            )
        }
    }

    // ── Merchant ─────────────────────────────────────────────────────────────────────

    /**
     * Header noise that is never a merchant name. ML Kit orders text blocks geometrically
     * rather than in reading order, so the first line is frequently a phone number, an address
     * or a greeting rather than the shop.
     */
    private val NOT_A_MERCHANT = Regex(
        """(?i)^(""" +
            """receipt|invoice|customer\s+copy|merchant\s+copy|welcome.*|thank\s*you.*|""" +
            """order\s*#?.*|store\s*#?\s*\d+.*|reg(?:ister)?\s*#?\s*\d+.*|""" +
            """(?:tel|phone|ph)[:.\s].*|www\..*|https?://.*|""" +
            """[\d\s()+\-]{7,}|""" +
            """\d{1,6}\s+\w+.*\b(?:st|street|rd|road|ave|avenue|blvd|ln|lane|dr|drive|hwy|suite|ste|unit)\b\.?""" +
            """)$""",
    )

    private const val MERCHANT_SEARCH_LINES = 5
    private val LETTER = Regex("""\p{L}""")

    private fun extractMerchant(lines: List<String>): String? {
        val qualifying = lines.take(MERCHANT_SEARCH_LINES).firstOrNull { line ->
            val cleaned = clean(line)
            !NOT_A_MERCHANT.matches(cleaned) &&
                LETTER.findAll(cleaned).take(2).count() == 2 &&
                cleaned.length in 2..40
        }
        // Never regress to null where the old "first non-blank line" rule returned something.
        return (qualifying ?: lines.firstOrNull())?.let { clean(it) }
    }

    /**
     * Trim, collapse internal whitespace, strip trailing punctuation — and deliberately leave
     * the casing alone. Title-casing reads better for STARBUCKS but breaks IKEA and H&M, and
     * this string feeds both `CategoryPredictionEngine` and merchant autocomplete, where a
     * stable spelling matters more than a pretty one.
     */
    private fun clean(line: String): String =
        line.trim().replace(Regex("""\s+"""), " ").trimEnd('.', ',', ':', ';', '-')

    // ── Date ─────────────────────────────────────────────────────────────────────────

    private val ISO = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
    private val NUMERIC = Regex("""\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})\b""")
    private val DMY_TEXT = Regex(
        """(?i)\b(\d{1,2})\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?,?\s*(\d{2,4})\b""",
    )
    /**
     * The separator between day and year is mandatory. Without it this pattern matches
     * "March 2024" by splitting the year into day=20 / year=24, stealing "4 March 2024" from
     * [DMY_TEXT] and yielding a date three weeks off.
     */
    private val MDY_TEXT = Regex(
        """(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s*""" +
            """(\d{1,2})(?:st|nd|rd|th)?(?:,\s*|\s+)(\d{2,4})(?!\d)""",
    )
    private val MONTHS = listOf(
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
    )

    /**
     * Scans top-down and takes the first date that survives [inSanityWindow]: the purchase date
     * is printed in the header, while the footer often carries a card expiry like `12/28` that
     * a bottom-up scan would pick up instead.
     */
    private fun extractDate(lines: List<String>, today: LocalDate, dayFirst: Boolean): LocalDate? {
        for (line in lines) {
            val parsed = parseIso(line)
                ?: parseTextual(line, today)
                ?: parseNumeric(line, today, dayFirst)
            if (parsed != null && inSanityWindow(parsed, today)) return parsed
        }
        return null
    }

    private fun parseIso(line: String): LocalDate? = ISO.find(line)?.let { m ->
        dateOrNull(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }

    private fun parseTextual(line: String, today: LocalDate): LocalDate? {
        MDY_TEXT.find(line)?.let { m ->
            val month = MONTHS.indexOf(m.groupValues[1].lowercase()) + 1
            return dateOrNull(fullYear(m.groupValues[3].toInt(), today), month, m.groupValues[2].toInt())
        }
        DMY_TEXT.find(line)?.let { m ->
            val month = MONTHS.indexOf(m.groupValues[2].lowercase()) + 1
            return dateOrNull(fullYear(m.groupValues[3].toInt(), today), month, m.groupValues[1].toInt())
        }
        return null
    }

    private fun parseNumeric(line: String, today: LocalDate, dayFirst: Boolean): LocalDate? {
        val m = NUMERIC.find(line) ?: return null
        val first = m.groupValues[1].toInt()
        val second = m.groupValues[2].toInt()
        val year = fullYear(m.groupValues[3].toInt(), today)
        // 25/12 can only be day-first; 03/25 can only be month-first. Genuinely ambiguous
        // pairs fall back to the caller's convention.
        val dayIsFirst = when {
            first > 12 -> true
            second > 12 -> false
            else -> dayFirst
        }
        return if (dayIsFirst) dateOrNull(year, second, first) else dateOrNull(year, first, second)
    }

    private fun fullYear(year: Int, today: LocalDate): Int = when {
        year >= 100 -> year
        2000 + year <= today.year + 1 -> 2000 + year
        else -> 1900 + year
    }

    /** kotlinx-datetime throws on 2024-02-31, which is exactly the garbage OCR produces. */
    private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate(year, month, day) }.getOrNull()

    /** A receipt dated 2087 is a misread loyalty number, not a date. */
    private fun inSanityWindow(date: LocalDate, today: LocalDate): Boolean =
        date <= today.plus(DatePeriod(days = 1)) && date >= today.minus(DatePeriod(years = 2))
}
