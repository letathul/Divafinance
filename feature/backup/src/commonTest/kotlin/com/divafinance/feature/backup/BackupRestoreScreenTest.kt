package com.divafinance.feature.backup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BackupRestoreScreenTest {

    @Test
    fun progressDialogDisplaysTitle() = runComposeUiTest {
        setContent {
            BackupProgressDialog(
                title = "Exporting",
                message = "Creating backup of your financial data...",
            )
        }
        onNodeWithText("Exporting").assertIsDisplayed()
    }
}
