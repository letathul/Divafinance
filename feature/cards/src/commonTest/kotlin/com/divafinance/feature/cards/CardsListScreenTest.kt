package com.divafinance.feature.cards

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CardsListScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            CardsListScreen()
        }
        onNodeWithText("Cards").assertIsDisplayed()
    }
}
