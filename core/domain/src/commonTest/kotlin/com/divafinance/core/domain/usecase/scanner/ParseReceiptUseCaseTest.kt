package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.model.enums.ReceiptStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParseReceiptUseCaseTest {

    private val receiptRepo = FakeReceiptRepository()
    private val useCase = ParseReceiptUseCase(receiptRepo)

    @Test
    fun parsesReceiptWithTotal() = runTest {
        val ocrText = """
            Starbucks Coffee
            Latte x1 $5.50
            Total: $5.50
        """.trimIndent()

        val result = useCase("/images/receipt.jpg", ocrText)

        assertNotNull(result.id)
        assertEquals("/images/receipt.jpg", result.imagePath)
        assertEquals("Starbucks Coffee", result.merchantName)
        assertEquals(5.50, result.totalAmount)
        assertEquals(ReceiptStatus.PROCESSED, result.status)
    }

    @Test
    fun setsStatusPendingWhenNoTotal() = runTest {
        val ocrText = """
            Some Store
            Items purchased
            Thank you
        """.trimIndent()

        val result = useCase("/images/receipt.jpg", ocrText)

        assertNull(result.totalAmount)
        assertEquals(ReceiptStatus.PENDING, result.status)
    }

    @Test
    fun extractsMerchantFromFirstLine() = runTest {
        val result = useCase("/img.jpg", "McDonald's\nBig Mac\nTotal: $8.99")

        assertEquals("McDonald's", result.merchantName)
    }

    @Test
    fun savesReceiptToRepository() = runTest {
        useCase("/img.jpg", "Store\nTotal: $10.00")

        val receipts = receiptRepo.getReceipts()
        assertEquals(1, receipts.size)
    }

    @Test
    fun parsesTotalWithCommas() = runTest {
        val result = useCase("/img.jpg", "Store\nTotal: $1,234.56")

        assertEquals(1234.56, result.totalAmount)
    }
}
