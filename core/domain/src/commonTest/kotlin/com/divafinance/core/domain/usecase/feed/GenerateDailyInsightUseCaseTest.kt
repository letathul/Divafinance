package com.divafinance.core.domain.usecase.feed

import com.divafinance.core.domain.fake.FakeFeedRepository
import com.divafinance.core.domain.fake.FakeTransactionRepository
import com.divafinance.core.domain.fake.TestData
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateDailyInsightUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val feedRepo = FakeFeedRepository()
    private val useCase = GenerateDailyInsightUseCase(txRepo, feedRepo)

    @Test
    fun generatesBotInsightPost() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val recentDate = today.minus(2, DateTimeUnit.DAY)

        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 50.0, date = recentDate, category = SpendingCategory.DINING),
            TestData.transaction(id = "2", amount = 100.0, date = recentDate, category = SpendingCategory.TRAVEL),
        ))

        useCase()

        val posts = feedRepo.getPosts()
        assertEquals(1, posts.size)
        assertEquals(FeedPostType.BOT_INSIGHT, posts[0].type)
        assertEquals("Weekly Spending Summary", posts[0].title)
    }

    @Test
    fun includesSpendingSummary() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val recentDate = today.minus(1, DateTimeUnit.DAY)

        txRepo.setTransactions(listOf(
            TestData.transaction(id = "1", amount = 75.0, date = recentDate),
        ))

        useCase()

        val post = feedRepo.getPosts().first()
        assertTrue(post.body.contains("1 transactions"))
        assertTrue(post.body.contains("75.00"))
    }

    @Test
    fun doesNothingForNoTransactions() = runTest {
        useCase()

        assertTrue(feedRepo.getPosts().isEmpty())
    }
}
