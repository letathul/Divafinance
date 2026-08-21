package com.divafinance.feature.graphs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.ui.component.chart.SpendingPieChart
import com.divafinance.core.ui.component.chart.ThresholdBarChart
import com.divafinance.core.ui.component.chart.TrendLineChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphsDashboardScreen(
    onNavigateToThresholdConfig: () -> Unit = {},
    viewModel: GraphsViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Insights") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ChartTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimePeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        label = { Text(period.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state.selectedTab) {
                ChartTab.PIE -> {
                    if (state.spendingSlices.isEmpty() && !state.isLoading) {
                        EmptyChartMessage("No spending data for this period")
                    } else {
                        SpendingPieChart(
                            slices = state.spendingSlices.toChartSlices(),
                            totalSpending = state.totalSpending,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                ChartTab.BAR -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Spending vs Thresholds",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            androidx.compose.material3.TextButton(
                                onClick = onNavigateToThresholdConfig,
                            ) {
                                Text("Configure")
                            }
                        }
                        ThresholdBarChart(
                            data = state.thresholdData.toChartBars(),
                            limitCaption = "Dashed line = threshold limit",
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                ChartTab.LINE -> {
                    if (state.trendData.isEmpty() && !state.isLoading) {
                        EmptyChartMessage("No trend data available")
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = "Monthly Spending Trend",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TrendLineChart(
                                data = state.trendData.toChartPoints(),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EmptyChartMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
