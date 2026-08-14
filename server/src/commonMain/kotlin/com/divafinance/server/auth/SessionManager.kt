package com.divafinance.server.auth

import kotlinx.serialization.Serializable

@Serializable
data class DivaSession(
    val token: String,
    val authenticated: Boolean = false,
)
