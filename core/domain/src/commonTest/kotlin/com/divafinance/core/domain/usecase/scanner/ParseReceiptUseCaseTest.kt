package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.model.enums.ReceiptStatus
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
        // PENDING even though it parsed cleanly — PROCESSED now means "linked to a
        // transaction", which only ConfirmReceiptUseCase can do.
        assertEquals(ReceiptStatus.PENDING, result.status)
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

    @Test
    fun persistsTheParsedDate() = runTest {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val result = useCase("/img.jpg", "Store\n$today\nTotal: $10.00")

        assertEquals(today, result.date)
    }

    @Test
    fun marksStatusFailedWhenOcrProducedNothing() = runTest {
        // The engine returns empty text rather than throwing when it can't read an image.
        val result = useCase("/img.jpg", "")

        assertEquals(ReceiptStatus.FAILED, result.status)
    }
}
