package com.divafinance.feature.scanner

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ScannerScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            ScannerScreen()
        }
        onNodeWithText("Scanner").assertIsDisplayed()
    }
}
