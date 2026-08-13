package com.divafinance.feature.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DashboardScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            DashboardScreen()
        }
        onNodeWithText("Dashboard").assertIsDisplayed()
    }
}
