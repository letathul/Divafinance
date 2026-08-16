package com.divafinance.feature.quickadd

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaTextField

/**
 * Bottom sheet for logging a transaction in as few taps as possible: type an amount on the
 * keypad, accept the pre-selected card and category, save.
 *
 * Everything beyond the amount has a usable default, so the common path never requires
 * scrolling or the soft keyboard. Merchant, note and card sit behind "Add details".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permissionRequester = rememberLocationPermissionRequester()

    LaunchedEffect(Unit) { viewModel.onOpened() }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        QuickAddSheetContent(
            state = state,
            onDigit = viewModel::onDigit,
            onOperator = viewModel::onOperator,
            onBackspace = viewModel::onBackspace,
            onTypeChange = viewModel::onTypeChange,
            onCategoryChange = viewModel::onCategoryChange,
            onToggleAllCategories = viewModel::onToggleAllCategories,
            onToggleDetails = viewModel::onToggleDetails,
            onMerchantChange = viewModel::onMerchantChange,
            onMerchantSuggestionPicked = viewModel::onMerchantSuggestionPicked,
            onNoteChange = viewModel::onNoteChange,
            onCardChange = viewModel::onCardChange,
            onDayChange = viewModel::onDayChange,
            onLocationToggled = { enabled ->
                if (enabled) {
                    // Only ever prompted from an explicit opt-in, never on sheet open.
                    permissionRequester.request { granted ->
                        viewModel.onLocationToggled(enabled = true, permissionGranted = granted)
                    }
                } else {
                    viewModel.onLocationToggled(enabled = false, permissionGranted = false)
                }
            },
            onLocationNameChange = viewModel::onLocationNameChange,
            onNearbyPlacePicked = viewModel::onNearbyPlacePicked,
            onSave = viewModel::save,
        )
    }
}

/**
 * Stateless body, split out so it can be driven directly from tests and previews without
 * a ViewModel or a sheet host. Callbacks default to no-ops for exactly that reason; the
 * real call site above passes every one.
 */
@Composable
internal fun QuickAddSheetContent(
    state: QuickAddUiState,
    onDigit: (Char) -> Unit = {},
    onOperator: (Char) -> Unit = {},
    onBackspace: () -> Unit = {},
    onTypeChange: (TransactionType) -> Unit = {},
    onCategoryChange: (SpendingCategory) -> Unit = {},
    onToggleAllCategories: () -> Unit = {},
    onToggleDetails: () -> Unit = {},
    onMerchantChange: (String) -> Unit = {},
    onMerchantSuggestionPicked: (String) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onCardChange: (String?) -> Unit = {},
    onDayChange: (QuickAddDay) -> Unit = {},
    onLocationToggled: (Boolean) -> Unit = {},
    onLocationNameChange: (String) -> Unit = {},
    onNearbyPlacePicked: (NearbyPlace) -> Unit = {},
    onSave: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AmountDisplay(state)

        TypeRow(state.type, onTypeChange)

        CategoryRow(state, onCategoryChange, onToggleAllCategories)

        DayRow(state.day, onDayChange)

        CalculatorKeypad(
            onDigit = onDigit,
            onOperator = onOperator,
            onBackspace = onBackspace,
        )

        DetailsSection(
            state = state,
            onToggleDetails = onToggleDetails,
            onMerchantChange = onMerchantChange,
            onMerchantSuggestionPicked = onMerchantSuggestionPicked,
            onNoteChange = onNoteChange,
            onCardChange = onCardChange,
        )

        LocationSection(
            state = state,
            onLocationToggled = onLocationToggled,
            onLocationNameChange = onLocationNameChange,
            onNearbyPlacePicked = onNearbyPlacePicked,
        )

        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        DivaButton(
            text = if (state.isSaving) "Saving..." else "Save",
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Shows the raw expression above its running total, so "12+8" stays visible and readable
 * while the total updates underneath.
 */
@Composable
private fun AmountDisplay(state: QuickAddUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.previewAmount?.toFixed(2) ?: "0.00",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        // Only worth showing once it is an actual sum rather than a repeat of the total.
        if (state.expression.any { it in "+-*/" }) {
            Text(
                text = state.expression,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypeRow(type: TransactionType, onTypeChange: (TransactionType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChip(
            label = "Expense",
            selected = type == TransactionType.DEBIT,
            onClick = { onTypeChange(TransactionType.DEBIT) },
        )
        CategoryChip(
            label = "Income",
            selected = type == TransactionType.CREDIT,
            onClick = { onTypeChange(TransactionType.CREDIT) },
        )
    }
}

@Composable
private fun CategoryRow(
    state: QuickAddUiState,
    onCategoryChange: (SpendingCategory) -> Unit,
    onToggleAllCategories: () -> Unit,
) {
    val shown = if (state.showAllCategories) {
        SpendingCategory.entries.toList()
    } else {
        // Keep the current pick visible even when it is not one of the suggestions.
        (state.suggestedCategories + state.category).distinct()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        shown.forEach { category ->
            CategoryChip(
                label = category.displayName,
                selected = state.category == category,
                onClick = { onCategoryChange(category) },
            )
        }
        CategoryChip(
            label = if (state.showAllCategories) "Less" else "More",
            onClick = onToggleAllCategories,
        )
    }
}

@Composable
private fun DayRow(day: QuickAddDay, onDayChange: (QuickAddDay) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAddDay.entries.forEach { option ->
            CategoryChip(
                label = option.label,
                selected = day == option,
                onClick = { onDayChange(option) },
            )
        }
    }
}

/**
 * Location is opt-in per entry and never blocks saving. Turning the switch on is what
 * triggers the OS prompt; everything below only appears once there is a fix.
 */
@Composable
private fun LocationSection(
    state: QuickAddUiState,
    onLocationToggled: (Boolean) -> Unit,
    onLocationNameChange: (String) -> Unit,
    onNearbyPlacePicked: (NearbyPlace) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Add location", style = MaterialTheme.typography.labelLarge)
        Switch(
            checked = state.locationEnabled,
            onCheckedChange = onLocationToggled,
        )
    }

    if (state.isLocatingNow) {
        Text(
            text = "Finding your location…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (state.locationUnavailable) {
        Text(
            text = "Location isn't available. You can still save without it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (state.location == null) return

    // Premium: shops from the user's own history near this point.
    if (state.nearbyPlaces.isNotEmpty()) {
        Text("Nearby", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            state.nearbyPlaces.forEach { place ->
                CategoryChip(
                    label = place.name,
                    selected = state.locationName == place.name,
                    onClick = { onNearbyPlacePicked(place) },
                )
            }
        }
    }

    // Whatever was reverse-geocoded is a starting point, not the final answer.
    DivaTextField(
        value = state.locationName,
        onValueChange = onLocationNameChange,
        label = "Place",
    )
}

@Composable
private fun DetailsSection(
    state: QuickAddUiState,
    onToggleDetails: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onMerchantSuggestionPicked: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCardChange: (String?) -> Unit,
) {
    CategoryChip(
        label = if (state.showDetails) "Hide details" else "Add details",
        onClick = onToggleDetails,
    )

    if (!state.showDetails) return

    DivaTextField(
        value = state.merchantName,
        onValueChange = onMerchantChange,
        label = "Merchant",
    )

    // Shops repeat, so offering past ones saves most of the typing — and picking one
    // sharpens the category prediction at the same time.
    if (state.merchantSuggestions.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            state.merchantSuggestions.forEach { suggestion ->
                CategoryChip(
                    label = suggestion,
                    onClick = { onMerchantSuggestionPicked(suggestion) },
                )
            }
        }
    }
    DivaTextField(
        value = state.note,
        onValueChange = onNoteChange,
        label = "Note",
    )

    if (state.cards.isNotEmpty() && state.type == TransactionType.DEBIT) {
        Text("Paid with", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            CategoryChip(
                label = "Cash",
                selected = state.selectedCardId == null,
                onClick = { onCardChange(null) },
            )
            state.cards.forEach { card ->
                CategoryChip(
                    label = card.name,
                    selected = state.selectedCardId == card.id,
                    onClick = { onCardChange(card.id) },
                )
            }
        }
    }
}
