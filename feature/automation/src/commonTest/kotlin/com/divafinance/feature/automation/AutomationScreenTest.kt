package com.divafinance.feature.automation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AutomationScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            AutomationScreen()
        }
        onNodeWithText("Automations").assertIsDisplayed()
    }
}
