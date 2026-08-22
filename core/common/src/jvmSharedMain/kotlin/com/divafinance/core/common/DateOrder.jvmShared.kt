package com.divafinance.core.common

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Read off the locale's own SHORT date pattern rather than a country allow-list — `en-GB` and
 * `en-US` differ, so the language alone never answers this.
 */
actual fun localePrefersDayFirst(): Boolean {
    val pattern = runCatching {
        (DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) as? SimpleDateFormat)
            ?.toPattern()
    }.getOrNull() ?: return false
    val day = pattern.indexOf('d')
    val month = pattern.indexOf('M')
    // A pattern missing either field tells us nothing; fall back to month-first.
    return day >= 0 && month >= 0 && day < month
}
