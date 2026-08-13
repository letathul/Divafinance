package com.divafinance.core.domain.usecase.feed

import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.TransactionType
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PostTransactionToFeedUseCase(
    private val feedRepository: FeedRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(transaction: Transaction) {
        val verb = when (transaction.type) {
            TransactionType.DEBIT -> "spent"
            TransactionType.CREDIT -> "received"
        }
        val merchant = transaction.merchantName?.let { " at $it" } ?: ""

        val post = FeedPost(
            id = Uuid.random().toString(),
            type = FeedPostType.TRANSACTION,
            title = "${transaction.category.displayName} Transaction",
            body = "You $verb ${transaction.currency} ${transaction.amount}$merchant.",
            transactionId = transaction.id,
            createdAt = Clock.System.now(),
        )
        feedRepository.insert(post)
    }
}
