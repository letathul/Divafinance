package com.divafinance.core.common

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

/**
 * `dateFormatFromTemplate` returns the pattern this locale would actually use for a numeric
 * day/month/year date — the Foundation equivalent of asking for the SHORT pattern on the JVM.
 */
actual fun localePrefersDayFirst(): Boolean {
    val pattern = NSDateFormatter.dateFormatFromTemplate(
        tmplate = "yMd",
        options = 0u,
        locale = NSLocale.currentLocale,
    ) ?: return false
    val day = pattern.indexOf('d')
    val month = pattern.indexOf('M')
    // A pattern missing either field tells us nothing; fall back to month-first.
    return day >= 0 && month >= 0 && day < month
}
