package com.divafinance.feature.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpendingMapScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            SpendingMapScreen()
        }
        onNodeWithText("Spending Map").assertIsDisplayed()
    }
}
