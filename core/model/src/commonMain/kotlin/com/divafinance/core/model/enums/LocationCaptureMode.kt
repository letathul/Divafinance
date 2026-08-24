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

    /**
     * Never read a fix, and never ask again.
     *
     * Written when the user declines the contextual sheet the first save puts in front of
     * them. Absent still means "never asked" — this is the difference between a question
     * not yet put and a question already answered no, and without it a decline would be
     * re-asked on the very next entry.
     */
    NEVER,
}
