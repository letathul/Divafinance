package com.divafinance.feature.feed

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FeedScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            FeedScreen()
        }
        onNodeWithText("Feed").assertIsDisplayed()
    }
}
