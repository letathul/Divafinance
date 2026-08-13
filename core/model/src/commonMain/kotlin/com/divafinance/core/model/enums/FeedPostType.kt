package com.divafinance.core.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class FeedPostType {
    TRANSACTION,
    BOT_INSIGHT,
    MILESTONE;
}
