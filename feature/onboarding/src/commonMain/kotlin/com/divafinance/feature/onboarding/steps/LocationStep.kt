package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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

@Composable
fun LocationStep(
    location: String,
    onLocationChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    OnboardingStepLayout(
        title = "Default Location",
        subtitle = "Optionally set a default location for your transactions. You can skip this step.",
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
                    text = if (location.isBlank()) "Skip" else "Next",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        DivaTextField(
            value = location,
            onValueChange = onLocationChanged,
            label = "City or Region",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    keyboard?.hide()
                    onNext()
                },
            ),
        )
    }
}

@Preview
@Composable
private fun LocationStepPreview() {
    DivaTheme {
        LocationStep(
            location = "San Francisco",
            onLocationChanged = {},
            onNext = {},
            onBack = {},
        )
    }
}
