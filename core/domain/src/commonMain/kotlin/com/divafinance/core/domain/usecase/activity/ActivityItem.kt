package com.divafinance.core.domain.usecase.activity

import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import com.divafinance.core.model.Transaction
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** What an activity row can be. Each renders differently. */
enum class ActivityKind(val displayName: String) {
    SPENDING("Spending"),
    DEBTS("Debts"),
    INSIGHTS("Insights"),
}

/**
 * One entry in the merged activity stream.
 *
 * Sealed rather than a single flattened row type, so each kind keeps the fields it
 * actually has and the UI branches exhaustively — adding a kind later becomes a compile
 * error at every render site rather than a silently missing case.
 */
sealed interface ActivityItem {
    val id: String
    val date: LocalDate
    val kind: ActivityKind

    data class Spend(
        val transaction: Transaction,
    ) : ActivityItem {
        override val id get() = "spend:${transaction.id}"
        override val date get() = transaction.date
        override val kind get() = ActivityKind.SPENDING

        /** True when part of this was fronted for other people. */
        val isShared: Boolean get() = transaction.othersShare > 0.0

        /** What the user actually consumed, once other people's shares are removed. */
        val ownShare: Double get() = transaction.amount - transaction.othersShare
    }

    data class Debt(
        val entry: LedgerEntry,
        /** Null when the person record has gone missing; the row still renders. */
        val person: Person?,
    ) : ActivityItem {
        override val id get() = "debt:${entry.id}"
        override val date get() = entry.date
        override val kind get() = ActivityKind.DEBTS

        val personName: String get() = person?.name ?: "Someone"

        /** Positive means this increased what they owe you. */
        val signedAmount: Double get() = entry.signedAmount
    }

    data class Insight(
        val post: FeedPost,
    ) : ActivityItem {
        override val id get() = "insight:${post.id}"
        override val date get() = post.createdAt.toLocalDateUtc()
        override val kind get() = ActivityKind.INSIGHTS
    }
}

/**
 * Feed posts carry only an `Instant`, so they need a date to sort alongside transactions
 * and ledger entries. UTC rather than the device zone keeps ordering stable if the user
 * travels; the discrepancy is at most a day at the boundary and only affects grouping.
 */
private fun Instant.toLocalDateUtc(): LocalDate = toLocalDateTime(TimeZone.UTC).date
