package com.divafinance.core.model

import com.divafinance.core.model.enums.FeedPostType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FeedPost(
    val id: String,
    val type: FeedPostType,
    val title: String,
    val body: String,
    val transactionId: String? = null,
    val metadata: String? = null,
    val createdAt: Instant,
)
