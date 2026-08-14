package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
        Text(
            text = "Select Your Currency",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose the base currency for tracking your finances.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(currencies) { currency ->
                val selected = currency.code == selectedCurrency
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCurrencySelected(currency.code) },
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
                        modifier = Modifier.padding(16.dp),
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
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.divafinance.core.ui.component.DivaOutlinedButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.weight(1f),
            )
            com.divafinance.core.ui.component.DivaButton(
                text = "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
