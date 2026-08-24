package com.divafinance.core.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * The full export payload.
 *
 * **Every field added here must have a default.** The reader sets `ignoreUnknownKeys`, so
 * an old app can read a new archive — but nothing rescues a *missing* field, and an
 * archive written before a field existed would fail to decode without one. Defaults are
 * what keep already-exported `.diva` files restorable.
 */
@Serializable
data class BackupArchive(
    val version: Int = CURRENT_VERSION,
    val createdAt: Instant,
    val accounts: List<Account>,
    val creditCards: List<CreditCard>,
    val rewardRules: List<CardRewardRule>,
    val transactions: List<Transaction>,
    val receipts: List<Receipt>,
    val feedPosts: List<FeedPost>,
    val settings: List<UserSettings>,
    val thresholds: List<GraphThreshold>,
    val people: List<Person> = emptyList(),
    val ledgerEntries: List<LedgerEntry> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList(),
) {
    companion object {
        /**
         * Bumped when the payload gains entities. Import refuses anything newer, since a
         * future archive may carry data this build would silently drop on the next export.
         */
        const val CURRENT_VERSION = 3
    }
}
