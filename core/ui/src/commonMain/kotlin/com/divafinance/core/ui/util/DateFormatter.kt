package com.divafinance.core.ui.util

import kotlinx.datetime.Instant
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
    val diff = today.toEpochDays() - date.toEpochDays()
    return when {
        diff == 0 -> "Today"
        diff == 1 -> "Yesterday"
        diff < 7 -> "$diff days ago"
        diff < 30 -> "${diff / 7} weeks ago"
        else -> formatDate(date)
    }
}
