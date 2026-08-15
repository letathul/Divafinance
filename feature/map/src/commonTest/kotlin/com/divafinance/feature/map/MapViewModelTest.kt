package com.divafinance.feature.map

import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapViewModelTest {

    private val sampleTransaction = Transaction(
        id = "t1",
        accountId = "a1",
        amount = 42.50,
        category = SpendingCategory.DINING,
        merchantName = "Cafe",
        date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        type = TransactionType.DEBIT,
        location = LocationTag(40.7128, -74.0060, "NYC"),
        createdAt = Clock.System.now(),
    )

    @Test
    fun mapUiStateDefaults() {
        val state = MapUiState()
        assertEquals(emptyList(), state.locationGroups)
        assertNull(state.selectedGroup)
        assertEquals(true, state.isLoading)
        assertNull(state.error)
        assertEquals(false, state.showTagDialog)
        assertNull(state.tagTransactionId)
    }

    @Test
    fun locationSpendingTotalAmount() {
        val group = LocationSpending(
            name = "NYC",
            location = LocationTag(40.7128, -74.0060, "NYC"),
            transactions = listOf(sampleTransaction, sampleTransaction.copy(id = "t2", amount = 57.50)),
            totalAmount = 100.0,
        )
        assertEquals("NYC", group.name)
        assertEquals(2, group.transactions.size)
        assertEquals(100.0, group.totalAmount)
    }

    @Test
    fun mapUiStateWithError() {
        val state = MapUiState(
            isLoading = false,
            error = "Network error",
        )
        assertEquals(false, state.isLoading)
        assertEquals("Network error", state.error)
    }

    @Test
    fun mapUiStateTagDialogState() {
        val state = MapUiState(
            showTagDialog = true,
            tagTransactionId = "t1",
        )
        assertEquals(true, state.showTagDialog)
        assertEquals("t1", state.tagTransactionId)
    }
}
