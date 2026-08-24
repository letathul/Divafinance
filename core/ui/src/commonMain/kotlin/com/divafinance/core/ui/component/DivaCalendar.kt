package com.divafinance.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.diva
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

/**
 * A month grid, and nothing else — no sheet, no confirm button, no state of its own
 * beyond which month is on screen.
 *
 * Selection fires on tap and the caller decides what that means, which is what lets the
 * date sheet close immediately instead of asking twice.
 *
 * [maxDate] greys out everything after it rather than hiding it, so the shape of the
 * month stays readable; a transaction cannot be dated into the future.
 */
@Composable
fun DivaCalendar(
    selected: LocalDate,
    displayedMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onMonthChange(displayedMonth.plusMonths(-1)) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = diva.muted,
                )
            }
            Text(
                "${MonthNames[displayedMonth.month.number - 1]} ${displayedMonth.year}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { onMonthChange(displayedMonth.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = diva.muted,
                )
            }
        }

        Row(Modifier.fillMaxWidth()) {
            WeekdayInitials.forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = diva.muted,
                )
            }
        }

        // Sunday-first, matching the reference design. `isoDayNumber` is Monday=1, so the
        // lead-in is that shifted by one and wrapped.
        val first = LocalDate(displayedMonth.year, displayedMonth.month, 1)
        val lead = first.dayOfWeek.isoDayNumber % 7
        val length = daysInMonth(displayedMonth.year, displayedMonth.month.number)
        val cells = lead + length
        val rows = (cells + 6) / 7

        for (row in 0 until rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (column in 0 until 7) {
                    val dayOfMonth = row * 7 + column - lead + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayOfMonth in 1..length) {
                            val date = LocalDate(displayedMonth.year, displayedMonth.month, dayOfMonth)
                            DayCell(
                                date = date,
                                isSelected = date == selected,
                                enabled = maxDate == null || date <= maxDate,
                                onSelect = onSelect,
                            )
                        } else {
                            Box(Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    Box(
        Modifier
            .padding(2.dp)
            .size(40.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) diva.accent else Color.Transparent)
            .then(
                if (enabled) Modifier.clickable { onSelect(date) } else Modifier
            )
            .semantics { contentDescription = date.toString() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isSelected -> MaterialTheme.colorScheme.surface
                !enabled -> diva.muted.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private val WeekdayInitials = listOf("S", "M", "T", "W", "T", "F", "S")

private val MonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapYear(year)) 29 else 28
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

/**
 * Month arithmetic without pulling in `kotlinx.datetime`'s period API, clamping the day so
 * stepping off the 31st into a shorter month lands on its last day rather than throwing.
 */
internal fun LocalDate.plusMonths(delta: Int): LocalDate {
    val zeroBased = (year * 12 + (month.number - 1)) + delta
    val newYear = zeroBased.floorDiv(12)
    val newMonth = zeroBased.mod(12) + 1
    val day = minOf(day, daysInMonth(newYear, newMonth))
    return LocalDate(newYear, newMonth, day)
}
