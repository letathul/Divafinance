package com.divafinance.feature.backup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BackupRestoreScreenTest {

    @Test
    fun displaysTitle() = runComposeUiTest {
        setContent {
            BackupRestoreScreen()
        }
        onNodeWithText("Backup & Restore").assertIsDisplayed()
    }
}
