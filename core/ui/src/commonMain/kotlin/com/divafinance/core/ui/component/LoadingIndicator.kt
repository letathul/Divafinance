package com.divafinance.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Sizes to its content. Callers that want it centred in a page pass
 * `Modifier.fillMaxSize()` — forcing that here made it impossible to use inline.
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onSurface,
            strokeWidth = 2.dp,
        )
    }
}

@Preview
@Composable
private fun LoadingIndicatorPreview() {
    DivaTheme {
        LoadingIndicator()
    }
}
