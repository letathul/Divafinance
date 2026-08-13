package com.divafinance.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

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
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
