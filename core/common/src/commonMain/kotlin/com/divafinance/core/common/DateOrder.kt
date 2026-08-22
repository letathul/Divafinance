package com.divafinance.core.common

/**
 * Whether this device's locale writes the day before the month in a short numeric date.
 *
 * Exists for one caller: receipt OCR, where `03/04` is genuinely ambiguous and the only signal
 * available is where the user is. Read once and passed into the parser rather than read inside
 * it, so extraction stays pure and testable.
 */
expect fun localePrefersDayFirst(): Boolean
