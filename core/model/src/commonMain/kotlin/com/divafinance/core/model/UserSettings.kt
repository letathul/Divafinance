package com.divafinance.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val key: String,
    val value: String,
) {
    companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_BASE_CURRENCY = "base_currency"
        const val KEY_DEFAULT_LOCATION = "default_location"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_IMPACT_THRESHOLD = "impact_threshold"
        const val KEY_POINTS_VALUE = "points_value"
        const val KEY_MILES_VALUE = "miles_value"
        const val KEY_SERVER_PORT = "server_port"

        /**
         * Light / dark / system, by [name]. Absent means the user has never chosen, which
         * is not the same as choosing "system" — see the legacy fallback below.
         */
        const val KEY_THEME_MODE = "theme_mode"

        /** Which accent sweep the UI spends its one accent on, by name. */
        const val KEY_ACCENT = "accent"

        /** Shown on the profile. Absent is fine — the screen falls back to a generic. */
        const val KEY_DISPLAY_NAME = "display_name"

        /**
         * What the user intends to spend in a month, as a plain number.
         *
         * Thresholds are stored as a percentage of total spending, which cannot be shown
         * as "$107 left" without a total to take a percentage *of*. This supplies it, as
         * a setting rather than a new table — absent simply means budgets render as
         * shares instead of amounts.
         */
        const val KEY_MONTHLY_BUDGET = "monthly_budget"

        /**
         * Superseded by [KEY_THEME_MODE]. Still *read* once so an install that predates
         * the three-way choice keeps the dark canvas it was already on; never written.
         */
        const val KEY_LEGACY_DARK_THEME = "dark_theme"

        /**
         * The account new transactions are booked against when the user does not pick one.
         * Written during onboarding, and repaired on launch for installs that predate it.
         */
        const val KEY_DEFAULT_ACCOUNT_ID = "default_account_id"

        /**
         * Pre-selected card in the quick-add sheet. Absent means "no card", which is a valid
         * choice (cash), so this is deliberately not defaulted to the first card.
         */
        const val KEY_DEFAULT_CARD_ID = "default_card_id"

        /**
         * Entitlement flag. Currently only ever set locally — there is no billing integration.
         */
        const val KEY_IS_PREMIUM = "is_premium"

        /**
         * Whether the add-expense sheet captures location on open or only when asked, by
         * [com.divafinance.core.model.enums.LocationCaptureMode] name. Absent means the
         * user has not been asked yet — see that enum.
         */
        const val KEY_LOCATION_CAPTURE_MODE = "location_capture_mode"

        /**
         * Lifecycle of the one-time demo offer. Absent means "not decided yet", which
         * is the only state in which the offer is shown. See DemoStatus.
         */
        const val KEY_DEMO_STATUS = "demo_status"
    }
}
