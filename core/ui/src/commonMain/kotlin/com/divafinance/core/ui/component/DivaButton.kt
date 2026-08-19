package com.divafinance.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The solid button: foreground-on-background, never accent-coloured.
 *
 * `primary` is the neutral in this theme precisely so this reads as white-on-dark (or
 * black-on-light) rather than picking up a tint. The accent is spent on the compose
 * button and one earned moment per screen, not on every call to action.
 */
@Composable
fun DivaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = Pill,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The tonal alternative: hairline border, transparent fill. */
@Composable
fun DivaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = Pill,
        border = BorderStroke(1.dp, diva.fgHair),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview
@Composable
private fun DivaButtonPreview() {
    DivaTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DivaButton(text = "Primary Action", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            DivaButton(text = "Disabled", onClick = {}, enabled = false)
            Spacer(modifier = Modifier.height(8.dp))
            DivaOutlinedButton(text = "Outlined Action", onClick = {})
        }
    }
}
