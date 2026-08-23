package com.divafinance.core.ui.util

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatDate(date: LocalDate): String {
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return "${date.year}-$month-$day"
}

fun formatInstant(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = instant.toLocalDateTime(timeZone)
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.year}-$month-$day $hour:$minute"
}

fun formatRelativeDate(date: LocalDate, today: LocalDate): String {
    // toEpochDays() returns Long as of kotlinx-datetime 0.7; a day delta always fits an Int.
    val diff = (today.toEpochDays() - date.toEpochDays()).toInt()
    return when {
        diff == 0 -> "Today"
        diff == 1 -> "Yesterday"
        diff < 7 -> "$diff days ago"
        diff < 30 -> "${diff / 7} weeks ago"
        else -> formatDate(date)
    }
}

private val MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "Aug 21" — the compact form a chip or a caption can carry next to other text. */
fun formatShortDate(date: LocalDate): String =
    "${MONTH_ABBREVIATIONS[date.monthNumber - 1]} ${date.dayOfMonth}"
