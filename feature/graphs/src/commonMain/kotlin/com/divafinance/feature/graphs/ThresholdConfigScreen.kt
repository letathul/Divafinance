package com.divafinance.feature.graphs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaOutlinedButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThresholdConfigScreen(
    onBack: () -> Unit,
    viewModel: GraphsViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.thresholdForm.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Threshold Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set spending threshold percentages per category. You'll see alerts when spending exceeds the threshold.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (form.isEditing) "Edit Threshold" else "Add Threshold",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SpendingCategory.entries.forEach { category ->
                                CategoryChip(
                                    label = category.displayName,
                                    selected = form.category == category,
                                    onClick = { viewModel.updateThresholdCategory(category) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = form.thresholdPercent,
                            onValueChange = { viewModel.updateThresholdPercent(it) },
                            label = { Text("Threshold %") },
                            placeholder = { Text("e.g. 25") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text("%") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (form.isEditing) {
                                DivaOutlinedButton(
                                    text = "Cancel",
                                    onClick = { viewModel.resetThresholdForm() },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            DivaButton(
                                text = if (form.isEditing) "Update" else "Add",
                                onClick = { viewModel.saveThreshold() },
                                modifier = Modifier.weight(1f),
                                enabled = form.thresholdPercent.toDoubleOrNull() != null &&
                                    (form.thresholdPercent.toDoubleOrNull() ?: 0.0) > 0,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Active Thresholds",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            val thresholdsWithConfig = state.thresholdData.filter { it.threshold != null }
            if (thresholdsWithConfig.isEmpty()) {
                item {
                    Text(
                        text = "No thresholds configured yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(thresholdsWithConfig, key = { it.category }) { data ->
                    ThresholdListItem(
                        data = data,
                        onEdit = { viewModel.startEditingThreshold(data) },
                        onDelete = { data.threshold?.let { viewModel.deleteThreshold(it.id) } },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ThresholdListItem(
    data: com.divafinance.core.domain.usecase.graphs.CategoryThresholdData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val threshold = data.threshold ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (data.isOverThreshold) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.category,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Limit: ${"%.0f".format(threshold.thresholdPercent)}% of total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                data.percentOfThreshold?.let { percentOfThreshold ->
                    Text(
                        text = if (data.isOverThreshold) {
                            "Over by ${"%.0f".format(percentOfThreshold - 100)}%"
                        } else {
                            "${"%.0f".format(percentOfThreshold)}% of limit used"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (data.isOverThreshold) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
