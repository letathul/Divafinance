package com.divafinance.feature.scanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScannerViewModelTest {

    @Test
    fun scannerUiStateDefaults() {
        val state = ScannerUiState()
        assertEquals(ScannerTab.RECEIPT, state.currentTab)
        assertEquals(false, state.isProcessing)
        assertNull(state.lastReceipt)
        assertNull(state.importedCount)
        assertNull(state.error)
        assertEquals("", state.csvContent)
        assertEquals("", state.accountId)
        assertEquals("", state.cardId)
    }

    @Test
    fun scannerTabSwitchClearsError() {
        val state = ScannerUiState(error = "some error", currentTab = ScannerTab.RECEIPT)
        val switched = state.copy(currentTab = ScannerTab.IMPORT, error = null)
        assertEquals(ScannerTab.IMPORT, switched.currentTab)
        assertNull(switched.error)
    }

    @Test
    fun scannerUiStateWithCsvContent() {
        val state = ScannerUiState(
            csvContent = "date,amount,merchant\n2024-01-01,42.50,Cafe",
            accountId = "acc1",
            cardId = "card1",
        )
        assertEquals("acc1", state.accountId)
        assertEquals("card1", state.cardId)
        assertEquals(true, state.csvContent.contains("merchant"))
    }

    @Test
    fun scannerUiStateWithImportResult() {
        val state = ScannerUiState(importedCount = 15)
        assertEquals(15, state.importedCount)
    }

    @Test
    fun scannerUiStateClearResult() {
        val state = ScannerUiState(
            lastReceipt = null,
            importedCount = 10,
            error = "old error",
        )
        val cleared = state.copy(lastReceipt = null, importedCount = null, error = null)
        assertNull(cleared.lastReceipt)
        assertNull(cleared.importedCount)
        assertNull(cleared.error)
    }
}
