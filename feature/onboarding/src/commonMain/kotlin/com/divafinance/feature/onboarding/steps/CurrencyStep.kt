package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private data class CurrencyOption(val code: String, val symbol: String, val name: String)

private val currencies = listOf(
    CurrencyOption("USD", "$", "US Dollar"),
    CurrencyOption("EUR", "€", "Euro"),
    CurrencyOption("GBP", "£", "British Pound"),
    CurrencyOption("JPY", "¥", "Japanese Yen"),
    CurrencyOption("INR", "₹", "Indian Rupee"),
    CurrencyOption("CAD", "CA$", "Canadian Dollar"),
    CurrencyOption("AUD", "A$", "Australian Dollar"),
)

@Composable
fun CurrencyStep(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingStepLayout(
        title = "Select Your Currency",
        subtitle = "Choose the base currency for tracking your finances.",
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
        // Rows of two rather than a LazyVerticalGrid: seven fixed options never justify
        // lazy layout, and a lazy list cannot be nested in the step's vertical scroll.
        currencies.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { currency ->
                    CurrencyCard(
                        currency = currency,
                        selected = currency.code == selectedCurrency,
                        onClick = { onCurrencySelected(currency.code) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps the odd last option at half width instead of stretching it.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CurrencyCard(
    currency: CurrencyOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = currency.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun CurrencyStepPreview() {
    DivaTheme {
        CurrencyStep(
            selectedCurrency = "USD",
            onCurrencySelected = {},
            onNext = {},
            onBack = {},
        )
    }
}
