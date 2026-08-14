package com.divafinance.feature.onboarding

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OnboardingScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            OnboardingScreen()
        }
        onNodeWithText("Onboarding").assertIsDisplayed()
    }
}
