package com.divafinance.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.theme.isCupertino
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DivaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.small,
        // Tonal fill with a hairline, matching DivaCard — an outlined field on a
        // near-black canvas reads as an empty box otherwise. On Cupertino the border
        // goes away entirely: a HIG field is a filled rectangle, not an outlined one.
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = diva.card,
            unfocusedContainerColor = diva.card,
            disabledContainerColor = diva.card,
            focusedBorderColor = if (isCupertino) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            },
            unfocusedBorderColor = if (isCupertino) Color.Transparent else diva.fgHair,
            disabledBorderColor = if (isCupertino) Color.Transparent else diva.fgHair,
            focusedLabelColor = diva.muted,
            unfocusedLabelColor = diva.muted,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Preview
@Composable
private fun DivaTextFieldPreview() {
    DivaTheme {
        DivaTextField(
            value = "Chase Sapphire",
            onValueChange = {},
            label = "Card Name",
            modifier = Modifier.padding(16.dp),
        )
    }
}
