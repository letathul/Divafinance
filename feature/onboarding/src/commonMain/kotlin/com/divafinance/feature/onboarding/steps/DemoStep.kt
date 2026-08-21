package com.divafinance.feature.onboarding.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The one and only place the demo is offered. Both buttons are terminal decisions —
 * the copy says so, because neither can be revisited later.
 */
@Composable
fun DemoStep(
    isCompleting: Boolean,
    onUseDemo: () -> Unit,
    onSkipDemo: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingStepLayout(
        title = "Try a Demo First?",
        subtitle = "We can load a few months of sample spending so you can explore every " +
            "screen before entering anything real.",
        actions = {
            DivaButton(
                text = if (isCompleting) "Loading sample data..." else "Load the demo",
                onClick = onUseDemo,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleting,
            )
            DivaOutlinedButton(
                text = "Start empty",
                onClick = onSkipDemo,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleting,
            )
            DivaOutlinedButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleting,
            )
        },
    ) {
        DivaCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("The sample data includes", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "3 credit cards with competing reward rules",
                    "~45 transactions across every category",
                    "Four months of history for the graphs",
                    "Geotagged spending for the map",
                    "Receipts, insights and threshold alerts",
                ).forEach { line ->
                    Text(
                        text = "•  $line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You can remove the demo any time from Settings. This is a one-time " +
                "offer either way — once you decide, we won't ask again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun DemoStepPreview() {
    DivaTheme {
        DemoStep(
            isCompleting = false,
            onUseDemo = {},
            onSkipDemo = {},
            onBack = {},
        )
    }
}
