package com.divafinance.core.domain.usecase.feed

import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.model.FeedPost
import kotlinx.coroutines.flow.Flow

class GetFeedPostsUseCase(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(): Flow<List<FeedPost>> = feedRepository.getAll()

    suspend fun getRecent(limit: Long = 50): List<FeedPost> = feedRepository.getRecent(limit)
}
