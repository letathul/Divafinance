package com.divafinance.feature.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.Hairline
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva

/**
 * Every attribute a transaction carries, as one scrollable sheet.
 *
 * Each group is a row of toggles over the same rule: tapping adds to a set, tapping again
 * removes, and an empty set means no restriction. That makes every group behave the same
 * way regardless of what it filters on, which is the only thing keeping a sheet this
 * dense learnable.
 */
@Composable
fun ReportFilterSheet(
    state: ReportUiState,
    onUpdate: ((ReportFilter) -> ReportFilter) -> Unit,
    onClearAll: () -> Unit,
) {
    val filter = state.filter

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.pad)
            .padding(bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Filter", style = MaterialTheme.typography.titleMedium)
            if (filter.isActive) {
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
        }

        DivaTextField(
            value = filter.query,
            onValueChange = { q -> onUpdate { it.copy(query = q) } },
            label = "Search merchant, note or place",
        )

        FilterGroup("Category") {
            SpendingCategory.entries.forEach { category ->
                CategoryChip(
                    label = category.displayName,
                    selected = category in filter.categories,
                    onClick = { onUpdate { it.copy(categories = it.categories.toggle(category)) } },
                )
            }
        }

        FilterGroup("Type") {
            TransactionType.entries.forEach { type ->
                CategoryChip(
                    label = if (type == TransactionType.DEBIT) "Expense" else "Income",
                    selected = type in filter.types,
                    onClick = { onUpdate { it.copy(types = it.types.toggle(type)) } },
                )
            }
        }

        if (state.availableCards.isNotEmpty()) {
            FilterGroup("Paid with") {
                state.availableCards.forEach { card ->
                    CategoryChip(
                        label = card.lastFour?.let { "${card.name} ·$it" } ?: card.name,
                        selected = card.id in filter.cardIds,
                        onClick = { onUpdate { it.copy(cardIds = it.cardIds.toggle(card.id)) } },
                    )
                }
            }
        }

        if (state.availableMerchants.isNotEmpty()) {
            FilterGroup("Merchant") {
                state.availableMerchants.forEach { merchant ->
                    CategoryChip(
                        label = merchant,
                        selected = merchant in filter.merchants,
                        onClick = { onUpdate { it.copy(merchants = it.merchants.toggle(merchant)) } },
                    )
                }
            }
        }

        if (state.availablePeople.isNotEmpty()) {
            FilterGroup("Split with") {
                state.availablePeople.forEach { (id, name) ->
                    CategoryChip(
                        label = name,
                        selected = id in filter.personIds,
                        onClick = { onUpdate { it.copy(personIds = it.personIds.toggle(id)) } },
                    )
                }
            }
        }

        Hairline()

        Meta("Amount")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            DivaTextField(
                value = filter.minAmount?.toFixed(2) ?: "",
                onValueChange = { v -> onUpdate { it.copy(minAmount = v.toDoubleOrNull()) } },
                label = "Min",
                modifier = Modifier.weight(1f),
            )
            DivaTextField(
                value = filter.maxAmount?.toFixed(2) ?: "",
                onValueChange = { v -> onUpdate { it.copy(maxAmount = v.toDoubleOrNull()) } },
                label = "Max",
                modifier = Modifier.weight(1f),
            )
        }

        FilterGroup("Only show") {
            TriStateChip("Split bills", filter.isSplit) { next ->
                onUpdate { it.copy(isSplit = next) }
            }
            TriStateChip("Recurring", filter.isRecurring) { next ->
                onUpdate { it.copy(isRecurring = next) }
            }
            TriStateChip("With receipt", filter.hasReceipt) { next ->
                onUpdate { it.copy(hasReceipt = next) }
            }
            TriStateChip("With location", filter.hasLocation) { next ->
                onUpdate { it.copy(hasLocation = next) }
            }
        }
    }
}

/**
 * Cycles ignore -> must -> must-not -> ignore. The excluded state is worth the third tap:
 * "everything except recurring" is a real question and cannot be asked any other way.
 */
@Composable
private fun TriStateChip(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    CategoryChip(
        label = when (value) {
            null -> label
            true -> label
            false -> "Not $label"
        },
        selected = value != null,
        onClick = {
            onChange(
                when (value) {
                    null -> true
                    true -> false
                    false -> null
                }
            )
        },
    )
}

@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Meta(title)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            content()
        }
    }
}

/** The row of removable chips under the report header. */
@Composable
fun ActiveFilterRow(
    state: ReportUiState,
    onUpdate: ((ReportFilter) -> ReportFilter) -> Unit,
    onClearAll: () -> Unit,
) {
    val filter = state.filter
    val peopleById = state.availablePeople.toMap()
    val cardsById = state.availableCards.associateBy { it.id }

    val chips = buildList {
        filter.categories.forEach { c ->
            add(c.displayName to { f: ReportFilter -> f.copy(categories = f.categories - c) })
        }
        filter.types.forEach { t ->
            val label = if (t == TransactionType.DEBIT) "Expense" else "Income"
            add(label to { f: ReportFilter -> f.copy(types = f.types - t) })
        }
        filter.cardIds.forEach { id ->
            add((cardsById[id]?.name ?: "Card") to { f: ReportFilter -> f.copy(cardIds = f.cardIds - id) })
        }
        filter.merchants.forEach { m ->
            add(m to { f: ReportFilter -> f.copy(merchants = f.merchants - m) })
        }
        filter.personIds.forEach { id ->
            add((peopleById[id] ?: "Person") to { f: ReportFilter -> f.copy(personIds = f.personIds - id) })
        }
        filter.minAmount?.let { min ->
            add("≥ ${min.toFixed(2)}" to { f: ReportFilter -> f.copy(minAmount = null) })
        }
        filter.maxAmount?.let { max ->
            add("≤ ${max.toFixed(2)}" to { f: ReportFilter -> f.copy(maxAmount = null) })
        }
        filter.isSplit?.let { v ->
            add((if (v) "Split" else "Not split") to { f: ReportFilter -> f.copy(isSplit = null) })
        }
        filter.isRecurring?.let { v ->
            add((if (v) "Recurring" else "Not recurring") to { f: ReportFilter -> f.copy(isRecurring = null) })
        }
        filter.hasReceipt?.let { v ->
            add((if (v) "With receipt" else "No receipt") to { f: ReportFilter -> f.copy(hasReceipt = null) })
        }
        filter.hasLocation?.let { v ->
            add((if (v) "With location" else "No location") to { f: ReportFilter -> f.copy(hasLocation = null) })
        }
        if (filter.query.isNotBlank()) {
            add("\"${filter.query}\"" to { f: ReportFilter -> f.copy(query = "") })
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.pad, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { (label, clear) ->
            CategoryChip(
                label = label,
                selected = true,
                onClick = { onUpdate(clear) },
            )
        }
        TextButton(onClick = onClearAll) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = diva.muted,
            )
            Text(" Clear", style = MaterialTheme.typography.bodySmall, color = diva.muted)
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value
