package com.divafinance.feature.transactions

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TransactionListScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            TransactionListScreen()
        }
        onNodeWithText("Transactions").assertIsDisplayed()
    }
}
