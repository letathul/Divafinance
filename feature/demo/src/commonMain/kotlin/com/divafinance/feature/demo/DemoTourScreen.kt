package com.divafinance.feature.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.theme.DivaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private data class TourEntry(val screen: String, val whatToLook: String)

private val tourEntries = listOf(
    TourEntry(
        "Dashboard",
        "Four months of salary and spending, so income, spending and net are all populated.",
    ),
    TourEntry(
        "Cards",
        "Three cards with deliberately overlapping reward rules — travel is 3x points on one " +
            "card and 5x miles on another, so Best Card has a real decision to make.",
    ),
    TourEntry(
        "Transactions",
        "Around 45 entries across every category, including recurring subscriptions and " +
            "notes you can open from the detail screen.",
    ),
    TourEntry(
        "Graphs",
        "Spending spread over four months with thresholds set so dining sits over its limit " +
            "and travel comfortably under it.",
    ),
    TourEntry(
        "Feed",
        "Bot insights, a milestone, and a transaction post — one of each type.",
    ),
    TourEntry(
        "Map",
        "Geotagged transactions clustered around the Bay Area and Seattle.",
    ),
    TourEntry(
        "Scanner",
        "Three receipts: one matched to a transaction, one pending, one failed.",
    ),
)

@Composable
fun DemoTourScreen(onBack: () -> Unit = {}) {
    DivaScaffold(
        title = "Demo Tour",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Sample data is loaded so you can try every screen without " +
                        "entering anything. Remove it from Settings whenever you're ready " +
                        "to start with your own data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(tourEntries) { entry ->
                DivaCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(entry.screen, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            entry.whatToLook,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Preview
@Composable
private fun DemoTourScreenPreview() {
    DivaTheme {
        DemoTourScreen()
    }
}
