package com.divafinance.core.domain.usecase.activity

import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.enums.FeedPostType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

/** What the stream is narrowed to. Null fields mean "no restriction". */
data class ActivityFilter(
    val kinds: Set<ActivityKind> = ActivityKind.entries.toSet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val query: String = "",
) {
    val isFiltered: Boolean
        get() = kinds.size != ActivityKind.entries.size ||
            from != null || to != null || query.isNotBlank()
}

/**
 * Spending, debts and insights as one reverse-chronological stream.
 *
 * Combines four flows so the list re-emits whenever any of them changes — logging a
 * transaction, settling a debt or generating an insight all update it without anything
 * needing to invalidate by hand.
 */
class GetActivityUseCase(
    private val transactionRepository: TransactionRepository,
    private val ledgerRepository: LedgerRepository,
    private val personRepository: PersonRepository,
    private val feedRepository: FeedRepository,
) {
    operator fun invoke(filter: ActivityFilter = ActivityFilter()): Flow<List<ActivityItem>> {
        return combine(
            transactionRepository.getAll(),
            ledgerRepository.getAll(),
            personRepository.getAll(),
            feedRepository.getAll(),
        ) { transactions, entries, people, posts ->
            val peopleById = people.associateBy { it.id }

            val spending = transactions.map { ActivityItem.Spend(it) }
            val debts = entries.map { ActivityItem.Debt(it, peopleById[it.personId]) }
            val insights = posts
                // A FeedPost of type TRANSACTION is an echo of a row already in this
                // stream — including it would show every spend twice.
                .filter { it.type != FeedPostType.TRANSACTION }
                .map { ActivityItem.Insight(it) }

            (spending + debts + insights)
                .filter { it.matches(filter) }
                .sortedWith(compareByDescending<ActivityItem> { it.date }.thenBy { it.id })
        }
    }

    private fun ActivityItem.matches(filter: ActivityFilter): Boolean {
        if (kind !in filter.kinds) return false
        if (filter.from != null && date < filter.from) return false
        if (filter.to != null && date > filter.to) return false

        val query = filter.query.trim().lowercase()
        if (query.isEmpty()) return true

        return searchableText().any { it.lowercase().contains(query) }
    }

    /** The text a row is findable by. Kept beside the item types so it stays in step. */
    private fun ActivityItem.searchableText(): List<String> = when (this) {
        is ActivityItem.Spend -> listOfNotNull(
            transaction.merchantName,
            transaction.note,
            transaction.category.displayName,
        )
        is ActivityItem.Debt -> listOfNotNull(personName, entry.note, entry.kind.displayName)
        is ActivityItem.Insight -> listOf(post.title, post.body)
    }
}
