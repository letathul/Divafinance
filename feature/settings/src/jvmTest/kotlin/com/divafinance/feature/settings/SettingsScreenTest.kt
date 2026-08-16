package com.divafinance.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            SettingsScreen()
        }
        onNodeWithText("Settings").assertIsDisplayed()
    }
}
