package com.divafinance.core.domain.usecase.reports

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The three windows the feed summarises and the report screen drills into.
 *
 * A period is always paired with an *anchor* date — any date inside it. That keeps the
 * pair serialisable into a nav route ("report/month/2026-08-17") without needing to
 * encode a start and an end separately, and makes "the previous one" a simple shift of
 * the anchor rather than range arithmetic.
 */
enum class ReportPeriod(val label: String) {
    DAY("Day"),
    MONTH("Month"),
    YEAR("Year");

    companion object {
        /** Parses the route segment. Falls back to [MONTH] rather than throwing. */
        fun fromName(name: String?): ReportPeriod =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MONTH
    }
}

/** First day of the period containing [anchor]. */
fun ReportPeriod.start(anchor: LocalDate): LocalDate = when (this) {
    ReportPeriod.DAY -> anchor
    ReportPeriod.MONTH -> LocalDate(anchor.year, anchor.monthNumber, 1)
    ReportPeriod.YEAR -> LocalDate(anchor.year, 1, 1)
}

/** Last day of the period containing [anchor], inclusive. */
fun ReportPeriod.end(anchor: LocalDate): LocalDate = when (this) {
    ReportPeriod.DAY -> anchor
    // Adding a month then stepping back a day handles month length and leap years
    // without a table.
    ReportPeriod.MONTH -> start(anchor).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    ReportPeriod.YEAR -> LocalDate(anchor.year, 12, 31)
}

/** An anchor inside the previous comparable period, for a "vs last month" delta. */
fun ReportPeriod.previousAnchor(anchor: LocalDate): LocalDate = when (this) {
    ReportPeriod.DAY -> anchor.minus(1, DateTimeUnit.DAY)
    ReportPeriod.MONTH -> start(anchor).minus(1, DateTimeUnit.MONTH)
    ReportPeriod.YEAR -> LocalDate(anchor.year - 1, 1, 1)
}

fun ReportPeriod.contains(anchor: LocalDate, date: LocalDate): Boolean =
    date >= start(anchor) && date <= end(anchor)

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/** Display title: "17 August 2026", "August 2026", "2026". */
fun ReportPeriod.title(anchor: LocalDate): String {
    val month = MONTH_NAMES[anchor.monthNumber - 1]
    return when (this) {
        ReportPeriod.DAY -> "${anchor.dayOfMonth} $month ${anchor.year}"
        ReportPeriod.MONTH -> "$month ${anchor.year}"
        ReportPeriod.YEAR -> anchor.year.toString()
    }
}

/** Short label for the previous period, used in "vs July" style deltas. */
fun ReportPeriod.previousLabel(anchor: LocalDate): String {
    val prev = previousAnchor(anchor)
    return when (this) {
        ReportPeriod.DAY -> "yesterday"
        ReportPeriod.MONTH -> MONTH_NAMES[prev.monthNumber - 1]
        ReportPeriod.YEAR -> prev.year.toString()
    }
}

fun monthName(monthNumber: Int): String = MONTH_NAMES[monthNumber - 1]

fun monthAbbreviation(monthNumber: Int): String = MONTH_NAMES[monthNumber - 1].take(3)
