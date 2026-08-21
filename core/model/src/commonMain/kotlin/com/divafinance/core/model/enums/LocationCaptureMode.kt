package com.divafinance.core.model.enums

/**
 * When the add-expense sheet reads the device position.
 *
 * Stored by [name] under `UserSettings.KEY_LOCATION_CAPTURE_MODE`. **Absent is a third
 * state**, not a default: it means the user has never been asked, which is the only
 * condition under which the sheet offers the choice. Defaulting a missing value to either
 * option here would either capture location without consent or silently retire the
 * question.
 */
enum class LocationCaptureMode {
    /** Read a fix as soon as the sheet opens, without waiting to be asked. */
    ALWAYS,

    /** Read a fix only when the user taps the place line. */
    ON_TAP,
}
