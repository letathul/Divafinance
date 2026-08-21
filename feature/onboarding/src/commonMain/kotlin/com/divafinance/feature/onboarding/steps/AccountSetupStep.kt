package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val accountTypes = listOf(
    "CHECKING" to "Checking Account",
    "SAVINGS" to "Savings Account",
    "WALLET" to "Digital Wallet",
    "CREDIT" to "Credit Account",
)

@Composable
fun AccountSetupStep(
    accountName: String,
    accountType: String,
    onAccountNameChanged: (String) -> Unit,
    onAccountTypeChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    OnboardingStepLayout(
        title = "Set Up Your Account",
        subtitle = "Create your first account to start tracking.",
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DivaOutlinedButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                DivaButton(
                    text = "Next",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        DivaTextField(
            value = accountName,
            onValueChange = onAccountNameChanged,
            label = "Account Name",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            // Done only dismisses here: the account type below still needs picking.
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Account Type",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        accountTypes.forEach { (type, label) ->
            val selected = type == accountType
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable { onAccountTypeChanged(type) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                ),
                border = if (selected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AccountSetupStepPreview() {
    DivaTheme {
        AccountSetupStep(
            accountName = "My Checking",
            accountType = "CHECKING",
            onAccountNameChanged = {},
            onAccountTypeChanged = {},
            onNext = {},
            onBack = {},
        )
    }
}
