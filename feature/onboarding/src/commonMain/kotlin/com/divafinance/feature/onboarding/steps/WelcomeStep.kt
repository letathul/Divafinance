package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to Diva Finance",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your personal finance companion. Track spending, manage credit cards, and get smart recommendations — all offline.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))
        com.divafinance.core.ui.component.DivaButton(
            text = "Get Started",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
