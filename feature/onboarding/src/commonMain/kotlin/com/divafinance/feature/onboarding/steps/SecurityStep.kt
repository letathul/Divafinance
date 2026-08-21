package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    val keyboard = LocalSoftwareKeyboardController.current
    val confirmFocus = remember { FocusRequester() }

    OnboardingStepLayout(
        title = "Secure Your App",
        subtitle = "Create a PIN to protect your financial data. At least 4 digits.",
        actions = {
            DivaButton(
                text = if (isCompleting) "Setting up..." else "Complete Setup",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleting,
            )
            DivaOutlinedButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        DivaTextField(
            value = pin,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) onPinChanged(it) },
            label = "Enter PIN",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { confirmFocus.requestFocus() }),
        )
        Spacer(Modifier.height(12.dp))
        DivaTextField(
            value = pinConfirm,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) onPinConfirmChanged(it) },
            label = "Confirm PIN",
            modifier = Modifier.fillMaxWidth().focusRequester(confirmFocus),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            // Done submits rather than only dismissing: the numeric keypad has no other
            // affordance, and the user's next move is the button underneath it anyway.
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboard?.hide()
                    if (!isCompleting) onComplete()
                },
            ),
        )
        if (pinError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pinError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview
@Composable
private fun SecurityStepPreview() {
    DivaTheme {
        SecurityStep(
            pin = "1234",
            pinConfirm = "1234",
            pinError = null,
            isCompleting = false,
            onPinChanged = {},
            onPinConfirmChanged = {},
            onComplete = {},
            onBack = {},
        )
    }
}
