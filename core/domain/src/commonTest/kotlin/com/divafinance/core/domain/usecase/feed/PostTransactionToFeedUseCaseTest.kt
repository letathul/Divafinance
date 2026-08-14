package com.divafinance.core.domain.usecase.feed

import com.divafinance.core.domain.fake.FakeFeedRepository
import com.divafinance.core.domain.fake.TestData
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostTransactionToFeedUseCaseTest {

    private val feedRepo = FakeFeedRepository()
    private val useCase = PostTransactionToFeedUseCase(feedRepo)

    @Test
    fun createsTransactionPost() = runTest {
        val tx = TestData.transaction(id = "tx-1", merchantName = "Pizza Place")

        useCase(tx)

        val posts = feedRepo.getPosts()
        assertEquals(1, posts.size)
        assertEquals(FeedPostType.TRANSACTION, posts[0].type)
        assertEquals("tx-1", posts[0].transactionId)
    }

    @Test
    fun includesMerchantNameInBody() = runTest {
        val tx = TestData.transaction(merchantName = "Starbucks")

        useCase(tx)

        val post = feedRepo.getPosts().first()
        assertTrue(post.body.contains("Starbucks"))
    }

    @Test
    fun handlesDebitTransaction() = runTest {
        val tx = TestData.transaction(type = TransactionType.DEBIT)

        useCase(tx)

        val post = feedRepo.getPosts().first()
        assertTrue(post.body.contains("spent"))
    }

    @Test
    fun handlesCreditTransaction() = runTest {
        val tx = TestData.transaction(type = TransactionType.CREDIT)

        useCase(tx)

        val post = feedRepo.getPosts().first()
        assertTrue(post.body.contains("received"))
    }

    @Test
    fun handlesNullMerchantName() = runTest {
        val tx = TestData.transaction(merchantName = null)

        useCase(tx)

        val post = feedRepo.getPosts().first()
        assertNotNull(post.body)
    }
}
