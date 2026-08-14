package com.divafinance.feature.feed.component

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatTimestamp(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    return if (local.date == now.date) {
        "Today at ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } else {
        "${local.monthNumber}/${local.dayOfMonth} at ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
}
