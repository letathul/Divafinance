package com.divafinance.feature.graphs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GraphsDashboardScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            GraphsDashboardScreen()
        }
        onNodeWithText("Graphs").assertIsDisplayed()
    }
}
