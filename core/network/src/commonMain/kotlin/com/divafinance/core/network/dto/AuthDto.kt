package com.divafinance.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val pin: String,
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val sessionToken: String? = null,
    val message: String? = null,
)
