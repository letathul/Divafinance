package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.model.UserSettings
import com.divafinance.core.testing.fake.FakeReceiptRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The safety property of the on-device model pass: it may fill a gap, and it may never
 * overwrite something the rules actually read off the receipt.
 */
class ReceiptExtractorMergeTest {

    private val receiptRepo = FakeReceiptRepository()
    private val settingsRepo = FakeSettingsRepository()

    private fun useCase(extractor: ReceiptExtractor) =
        ParseReceiptUseCase(receiptRepo, settingsRepo, extractor)

    private class StubExtractor(
        private val availability: ExtractorAvailability = ExtractorAvailability.READY,
        private val result: ExtractedReceipt? = null,
    ) : ReceiptExtractor {
        var calls = 0
            private set

        override suspend fun availability() = availability
        override suspend fun extract(ocrText: String): ExtractedReceipt? {
            calls++
            return result
        }
    }

    private class ThrowingExtractor : ReceiptExtractor {
        override suspend fun availability() = ExtractorAvailability.READY
        override suspend fun extract(ocrText: String): ExtractedReceipt =
            throw IllegalStateException("model exploded")
    }

    /**
     * The date is today's rather than a fixed one: the use case reads the real clock, and the
     * parser refuses anything more than two years old, so a hard-coded date silently stops
     * being parsed at all and the test would pass for the wrong reason.
     */
    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val readable = "GreenLeaf Market\nTotal 45.36\n$today"

    @Test
    fun neverOverwritesAValueTheRulesRead() = runTest {
        val extractor = StubExtractor(
            result = ExtractedReceipt(
                merchantName = "Somewhere Else",
                totalAmount = 999.99,
                date = LocalDate(2001, 1, 1),
            ),
        )

        val receipt = useCase(extractor)("/img.jpg", readable)

        assertEquals("GreenLeaf Market", receipt.merchantName)
        assertEquals(45.36, receipt.totalAmount)
        assertEquals(today, receipt.date)
    }

    @Test
    fun fillsAFieldTheRulesCouldNotRead() = runTest {
        // No recognisable total label, so the rules come back empty on the amount.
        val extractor = StubExtractor(result = ExtractedReceipt(totalAmount = 12.75))

        val receipt = useCase(extractor)("/img.jpg", "GreenLeaf Market\nthanks for visiting")

        assertEquals(12.75, receipt.totalAmount)
    }

    @Test
    fun suppliesItemsTheRulesNeverFound() = runTest {
        val extractor = StubExtractor(
            result = ExtractedReceipt(
                items = listOf(
                    ParsedLineItem(description = "Bananas", price = 3.20),
                    ParsedLineItem(description = "Coffee", price = 8.00),
                ),
            ),
        )

        val receipt = useCase(extractor)("/img.jpg", "GreenLeaf Market\nTotal 11.20")

        assertEquals(listOf("Bananas", "Coffee"), receipt.lineItems.map { it.description })
        assertEquals(listOf(0, 1), receipt.lineItems.map { it.position })
    }

    @Test
    fun keepsTheRulesItemsWhenBothFoundSome() = runTest {
        val extractor = StubExtractor(
            result = ExtractedReceipt(
                items = listOf(ParsedLineItem(description = "Invented", price = 1.0)),
            ),
        )

        val receipt = useCase(extractor)(
            "/img.jpg",
            "GreenLeaf Market\nBananas 3.20\nTotal 3.20",
        )

        assertEquals(listOf("Bananas"), receipt.lineItems.map { it.description })
    }

    @Test
    fun isNotCalledWhenTheSettingIsOff() = runTest {
        settingsRepo.set(UserSettings.KEY_SMART_RECEIPT_READING, "false")
        val extractor = StubExtractor(result = ExtractedReceipt(totalAmount = 12.75))

        val receipt = useCase(extractor)("/img.jpg", "GreenLeaf Market")

        assertEquals(0, extractor.calls)
        assertNull(receipt.totalAmount)
    }

    @Test
    fun isNotCalledWhenTheModelIsStillDownloading() = runTest {
        val extractor = StubExtractor(availability = ExtractorAvailability.DOWNLOADING)

        useCase(extractor)("/img.jpg", "GreenLeaf Market")

        assertEquals(0, extractor.calls)
    }

    @Test
    fun isNotCalledForAnEmptyScan() = runTest {
        // Nothing to extract from, and the receipt is already destined for FAILED.
        val extractor = StubExtractor()

        useCase(extractor)("/img.jpg", "")

        assertEquals(0, extractor.calls)
    }

    @Test
    fun aFailingModelLeavesTheRulesResultIntact() = runTest {
        val receipt = useCase(ThrowingExtractor())("/img.jpg", readable)

        assertEquals("GreenLeaf Market", receipt.merchantName)
        assertEquals(45.36, receipt.totalAmount)
    }

    @Test
    fun defaultsToTheRulesOnlyWhenNoExtractorIsWired() = runTest {
        val receipt = ParseReceiptUseCase(receiptRepo, settingsRepo)("/img.jpg", readable)

        assertEquals(45.36, receipt.totalAmount)
        assertTrue(receipt.lineItems.isEmpty())
    }
}
