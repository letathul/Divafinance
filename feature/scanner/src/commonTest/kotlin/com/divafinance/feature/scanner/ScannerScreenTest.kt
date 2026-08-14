package com.divafinance.feature.scanner

import kotlin.test.Test
import kotlin.test.assertEquals

class ScannerScreenTest {

    @Test
    fun scannerTabEnumValues() {
        assertEquals(2, ScannerTab.entries.size)
        assertEquals(ScannerTab.RECEIPT, ScannerTab.entries[0])
        assertEquals(ScannerTab.IMPORT, ScannerTab.entries[1])
    }

    @Test
    fun scannerUiStateDefaults() {
        val state = ScannerUiState()
        assertEquals(ScannerTab.RECEIPT, state.currentTab)
        assertEquals(false, state.isProcessing)
        assertEquals(null, state.lastReceipt)
        assertEquals(null, state.importedCount)
        assertEquals(null, state.error)
    }
}
