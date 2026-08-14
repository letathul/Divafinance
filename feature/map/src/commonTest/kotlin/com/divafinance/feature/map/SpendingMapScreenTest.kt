package com.divafinance.feature.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.theme.DivaTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpendingMapScreenTest {

    private val sampleTransaction = Transaction(
        id = "t1",
        accountId = "a1",
        amount = 42.50,
        category = SpendingCategory.DINING,
        merchantName = "Starbucks",
        date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        type = TransactionType.DEBIT,
        location = LocationTag(40.7128, -74.0060, "New York"),
        createdAt = Clock.System.now(),
    )

    @Test
    fun fallbackDisplaysEmptyState() = runComposeUiTest {
        setContent {
            DivaTheme {
                MapFallbackScreen(locationGroups = emptyList())
            }
        }
        onNodeWithText("No location-tagged transactions").assertIsDisplayed()
    }

    @Test
    fun fallbackDisplaysLocationGroups() = runComposeUiTest {
        val groups = listOf(
            LocationSpending(
                name = "New York",
                location = LocationTag(40.7128, -74.0060, "New York"),
                transactions = listOf(sampleTransaction),
                totalAmount = 42.50,
            ),
        )
        setContent {
            DivaTheme {
                MapFallbackScreen(locationGroups = groups)
            }
        }
        onNodeWithText("New York").assertIsDisplayed()
        onNodeWithText("1 transaction").assertIsDisplayed()
    }

    @Test
    fun locationDetailSheetDisplaysTransactions() = runComposeUiTest {
        val group = LocationSpending(
            name = "New York",
            location = LocationTag(40.7128, -74.0060, "New York"),
            transactions = listOf(sampleTransaction),
            totalAmount = 42.50,
        )
        setContent {
            DivaTheme {
                LocationDetailSheet(group = group)
            }
        }
        onNodeWithText("New York").assertIsDisplayed()
        onNodeWithText("Starbucks").assertIsDisplayed()
    }
}
