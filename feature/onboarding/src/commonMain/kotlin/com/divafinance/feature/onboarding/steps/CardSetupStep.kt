package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

private val cardNetworks = listOf(
    "VISA" to "Visa",
    "MASTERCARD" to "Mastercard",
    "AMEX" to "American Express",
    "DISCOVER" to "Discover",
    "OTHER" to "Other",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardSetupStep(
    cardName: String,
    cardNetwork: String,
    onCardNameChanged: (String) -> Unit,
    onCardNetworkChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    OnboardingStepLayout(
        title = "Add a Credit Card",
        subtitle = "Add your first card for reward tracking. You can add more later or skip this step.",
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
                    text = if (cardName.isBlank()) "Skip" else "Next",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        DivaTextField(
            value = cardName,
            onValueChange = onCardNameChanged,
            label = "Card Name",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            // Done only dismisses here: the network below still needs picking.
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Card Network",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        // Wraps rather than a single Row: five chips do not fit one line on a narrow
        // phone, and the old Row squeezed "American Express" down to an ellipsis.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cardNetworks.forEach { (code, label) ->
                val selected = code == cardNetwork
                Card(
                    modifier = Modifier.clickable { onCardNetworkChanged(code) },
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CardSetupStepPreview() {
    DivaTheme {
        CardSetupStep(
            cardName = "Chase Sapphire",
            cardNetwork = "VISA",
            onCardNameChanged = {},
            onCardNetworkChanged = {},
            onNext = {},
            onBack = {},
        )
    }
}
