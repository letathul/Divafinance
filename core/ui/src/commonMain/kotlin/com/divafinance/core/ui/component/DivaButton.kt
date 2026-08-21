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
import com.divafinance.core.ui.theme.isCupertino
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The solid call to action.
 *
 * It takes `primary` and so comes out right on both platforms without branching: on
 * Material `primary` is the neutral foreground, which is what has kept the accent to
 * twice a screen; on Cupertino it is the accent, because HIG tints every primary action.
 * Shape follows suit — a pill on Material, a 14dp rounded rectangle on iOS.
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
        modifier = modifier.fillMaxWidth().height(if (isCupertino) 50.dp else 52.dp),
        enabled = enabled,
        shape = if (isCupertino) MaterialTheme.shapes.medium else Pill,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The quieter alternative: hairline border over the card fill, tinted label. */
@Composable
fun DivaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(if (isCupertino) 50.dp else 52.dp),
        enabled = enabled,
        shape = if (isCupertino) MaterialTheme.shapes.medium else Pill,
        border = BorderStroke(diva.hairline, if (isCupertino) diva.separator else diva.fgHair),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = diva.card,
            // `primary` again: neutral on Material, the accent on Cupertino.
            contentColor = MaterialTheme.colorScheme.primary,
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
