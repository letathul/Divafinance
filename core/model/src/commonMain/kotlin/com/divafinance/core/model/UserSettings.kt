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
    }
}
