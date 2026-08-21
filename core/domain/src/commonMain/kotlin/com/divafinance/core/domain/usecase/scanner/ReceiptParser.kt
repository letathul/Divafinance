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
)

/**
 * Pure extraction from OCR text — no repository, no clock, no coroutine.
 *
 * `today` is a parameter rather than a `Clock.System` read so the sanity window on dates is
 * deterministic under test. OCR output is noisy in specific, predictable ways, and most of the
 * rules below exist to reject one of them; the tests name each case.
 */
object ReceiptParser {

    fun parse(ocrText: String, today: LocalDate, dayFirst: Boolean = false): ParsedReceipt {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        return ParsedReceipt(
            merchantName = extractMerchant(lines),
            totalAmount = extractTotal(lines),
            date = extractDate(lines, today, dayFirst),
        )
    }

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
        MONEY.findAll(text).lastOrNull()?.let { match ->
            val whole = match.groupValues[1].replace(",", "")
            return "$whole.${match.groupValues[2]}".toDoubleOrNull()
        }
        return MONEY_LOOSE.findAll(text).lastOrNull()
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
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
