package com.divafinance.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.diva
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CategoryChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = Pill,
        // Selection inverts fore- and background together rather than tinting, so
        // contrast never drops below the resting state.
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else diva.muted,
        border = if (selected) null else BorderStroke(1.dp, diva.fgHair),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Preview
@Composable
private fun CategoryChipPreview() {
    DivaTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(label = "Dining", selected = true)
            CategoryChip(label = "Travel", selected = false)
            CategoryChip(label = "Gas")
        }
    }
}
