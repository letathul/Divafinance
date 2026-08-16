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
         * Lifecycle of the one-time demo offer. Absent means "not decided yet", which
         * is the only state in which the offer is shown. See DemoStatus.
         */
        const val KEY_DEMO_STATUS = "demo_status"
    }
}
