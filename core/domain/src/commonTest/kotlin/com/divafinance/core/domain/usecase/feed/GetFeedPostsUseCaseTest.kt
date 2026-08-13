package com.divafinance.core.domain.usecase.feed

import com.divafinance.core.domain.fake.FakeFeedRepository
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetFeedPostsUseCaseTest {

    private val feedRepo = FakeFeedRepository()
    private val useCase = GetFeedPostsUseCase(feedRepo)

    @Test
    fun returnsAllPosts() = runTest {
        feedRepo.setPosts(listOf(
            FeedPost("1", FeedPostType.TRANSACTION, "Title", "Body", createdAt = Clock.System.now()),
            FeedPost("2", FeedPostType.BOT_INSIGHT, "Insight", "Data", createdAt = Clock.System.now()),
        ))

        val result = useCase().first()

        assertEquals(2, result.size)
    }

    @Test
    fun getRecentLimitsResults() = runTest {
        feedRepo.setPosts(List(10) {
            FeedPost("$it", FeedPostType.TRANSACTION, "Title $it", "Body", createdAt = Clock.System.now())
        })

        val result = useCase.getRecent(3)

        assertEquals(3, result.size)
    }

    @Test
    fun returnsEmptyWhenNoPosts() = runTest {
        val result = useCase().first()
        assertTrue(result.isEmpty())
    }
}
