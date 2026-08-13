package com.divafinance.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BackupArchive(
    val version: Int = 1,
    val createdAt: Instant,
    val accounts: List<Account>,
    val creditCards: List<CreditCard>,
    val rewardRules: List<CardRewardRule>,
    val transactions: List<Transaction>,
    val receipts: List<Receipt>,
    val feedPosts: List<FeedPost>,
    val settings: List<UserSettings>,
    val thresholds: List<GraphThreshold>,
)
