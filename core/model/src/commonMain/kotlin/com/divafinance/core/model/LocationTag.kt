package com.divafinance.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationTag(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
)
