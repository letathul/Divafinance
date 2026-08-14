package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SecurityStep(
    pin: String,
    pinConfirm: String,
    pinError: String?,
    isCompleting: Boolean,
    onPinChanged: (String) -> Unit,
    onPinConfirmChanged: (String) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
        Text(
            text = "Secure Your App",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Create a PIN to protect your financial data. At least 4 digits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        com.divafinance.core.ui.component.DivaTextField(
            value = pin,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) onPinChanged(it) },
            label = "Enter PIN",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        Spacer(Modifier.height(12.dp))
        com.divafinance.core.ui.component.DivaTextField(
            value = pinConfirm,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) onPinConfirmChanged(it) },
            label = "Confirm PIN",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        if (pinError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pinError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.divafinance.core.ui.component.DivaButton(
                text = if (isCompleting) "Setting up..." else "Complete Setup",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleting,
            )
            com.divafinance.core.ui.component.DivaOutlinedButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
