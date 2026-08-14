package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationStep(
    location: String,
    onLocationChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
        Text(
            text = "Default Location",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Optionally set a default location for your transactions. You can skip this step.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        com.divafinance.core.ui.component.DivaTextField(
            value = location,
            onValueChange = onLocationChanged,
            label = "City or Region",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
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
                text = if (location.isBlank()) "Skip" else "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
