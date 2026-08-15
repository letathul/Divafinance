package com.divafinance.core.domain.usecase.feed

import com.divafinance.core.common.toFixed
import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GenerateDailyInsightUseCase(
    private val transactionRepository: TransactionRepository,
    private val feedRepository: FeedRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val weekAgo = today.minus(7, DateTimeUnit.DAY)

        val transactions = transactionRepository.getByDateRange(weekAgo, today)
        if (transactions.isEmpty()) return

        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val totalSpent = debits.sumOf { it.amount }
        val txCount = debits.size
        val topCategory = debits.groupBy { it.category }
            .maxByOrNull { (_, txs) -> txs.sumOf { it.amount } }

        val title = "Weekly Spending Summary"
        val body = buildString {
            append("This week you made $txCount transactions totaling ${totalSpent.toFixed(2)}.")
            if (topCategory != null) {
                val catTotal = topCategory.value.sumOf { it.amount }
                append(" Your top category was ${topCategory.key.displayName}")
                append(" at ${catTotal.toFixed(2)}.")
            }
        }

        val post = FeedPost(
            id = Uuid.random().toString(),
            type = FeedPostType.BOT_INSIGHT,
            title = title,
            body = body,
            createdAt = Clock.System.now(),
        )
        feedRepository.insert(post)
    }
}
