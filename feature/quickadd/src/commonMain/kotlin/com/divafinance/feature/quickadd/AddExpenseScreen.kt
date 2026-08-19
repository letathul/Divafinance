package com.divafinance.feature.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.common.toMajorUnits
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CalculatorKeypad
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.CategoryPickerItem
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.GlassSurface
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Full-screen flow for logging a transaction in as few taps as possible: type an amount on
 * the keypad, accept the pre-selected card and category, save.
 *
 * Everything beyond the amount has a usable default, so the common path never requires the
 * soft keyboard. Merchant, note and split sit behind disclosure chips.
 */
@Composable
fun AddExpenseScreen(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel,
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val permissionRequester = rememberLocationPermissionRequester()

    LaunchedEffect(Unit) { viewModel.onOpened() }

    // The ViewModel decides *whether* to prompt; the launcher lives here because Android
    // needs an Activity result contract. Keyed on the nonce so every decision is acted on
    // exactly once, including the automatic capture that runs when the sheet opens.
    LaunchedEffect(state.permissionRequestNonce) {
        if (state.permissionRequestNonce > 0) {
            permissionRequester.request(viewModel::onLocationPermissionResult)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        AddExpenseTopBar(
            onClose = {
                viewModel.reset()
                onDismiss()
            },
            onScan = onOpenScanner,
        )
        AddExpenseContent(
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
            onWhereTapped = viewModel::onWhereTapped,
            onLocationCaptureModeChosen = viewModel::onLocationCaptureModeChosen,
            onLocationRationaleAccepted = viewModel::onLocationRationaleAccepted,
            onLocationPromptDismissed = viewModel::onLocationPromptDismissed,
            onLocationNameChange = viewModel::onLocationNameChange,
            onNearbyPlacePicked = viewModel::onNearbyPlacePicked,
            onSplitToggled = viewModel::onSplitToggled,
            onTipPercentChange = viewModel::onTipPercentChange,
            onAddSplitPerson = { name -> viewModel.onAddSplitPerson(name) },
            onRemoveSplitPerson = viewModel::onRemoveSplitPerson,
            onSave = viewModel::save,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun AddExpenseTopBar(onClose: () -> Unit, onScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.pad, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlassSurface(Modifier.size(40.dp).clickable(onClick = onClose)) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Discard this expense",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center).size(20.dp),
            )
        }
        Text("New expense", style = MaterialTheme.typography.titleMedium)
        GlassSurface(Modifier.size(40.dp).clickable(onClick = onScan)) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = "Scan a receipt instead",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center).size(20.dp),
            )
        }
    }
}

/**
 * Stateless body, split out so it can be driven directly from tests and previews without
 * a ViewModel or a navigation host. Callbacks default to no-ops for exactly that reason; the
 * real call site above passes every one.
 */
@Composable
internal fun AddExpenseContent(
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
    onWhereTapped: () -> Unit = {},
    onLocationCaptureModeChosen: (LocationCaptureMode) -> Unit = {},
    onLocationRationaleAccepted: () -> Unit = {},
    onLocationPromptDismissed: () -> Unit = {},
    onLocationNameChange: (String) -> Unit = {},
    onNearbyPlacePicked: (NearbyPlace) -> Unit = {},
    onSplitToggled: (Boolean) -> Unit = {},
    onTipPercentChange: (Double) -> Unit = {},
    onAddSplitPerson: (String) -> Unit = {},
    onRemoveSplitPerson: (String) -> Unit = {},
    onSave: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Outside the Column on purpose: a dialog still emits a zero-size node where it is
    // called, which `spacedBy` would turn into a stray 12dp gap while one is open.
    LocationPromptDialog(
        prompt = state.locationPrompt,
        onModeChosen = onLocationCaptureModeChosen,
        onRationaleAccepted = onLocationRationaleAccepted,
        onDismiss = onLocationPromptDismissed,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AmountDisplay(state, onWhereTapped)

        AccountSelector(state, onCardChange)

        CategoryPickerRow(state, onCategoryChange, onToggleAllCategories)

        Row(modifier= Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TypeRow(state.type, onTypeChange)

            DayRow(state.day, onDayChange)
        }

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
        )

        SplitSection(
            state = state,
            onSplitToggled = onSplitToggled,
            onTipPercentChange = onTipPercentChange,
            onAddSplitPerson = onAddSplitPerson,
            onRemoveSplitPerson = onRemoveSplitPerson,
        )

        LocationSection(
            state = state,
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
            text = if (state.isSaving) "Saving…" else "Add expense",
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
private fun AmountDisplay(state: QuickAddUiState, onWhereTapped: () -> Unit) {
    val amount = state.previewAmount
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Space.sm, bottom = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatCurrency(amount ?: 0.0),
            style = MaterialTheme.typography.displayLarge,
            // Greyed until there is a real figure, so the zero reads as a placeholder
            // rather than as an amount someone might save by accident.
            color = if (amount == null || amount == 0.0) {
                diva.muted
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
        )
        // Only worth showing once it is an actual sum rather than a repeat of the total.
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.expression.any { it in "+-*/" }) {
                Text(
                    text = state.expression,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = diva.muted,
                )
            }
            Spacer(modifier = Modifier.padding(4.dp))
            WhereLine(state, onWhereTapped)
        }
    }
}

/**
 * The caption under the amount, and the whole of the location UI's entry point.
 *
 * One control does three jobs depending on what has been settled — opt in, grant, refresh
 * — because to the user they are all "tell the app where I am". It reads as a label until
 * tapped, which is why it carries a pin and a spelled-out content description: nothing
 * else marks it as interactive.
 */
@Composable
private fun WhereLine(state: QuickAddUiState, onWhereTapped: () -> Unit) {
    val place = state.locationName.ifBlank { state.merchantName }
    val label = when {
        state.isLocatingNow -> "Finding you…"
        place.isNotBlank() -> place
        else -> "Where?"
    }

    Row(
        modifier = Modifier
            .clip(Pill)
            .clickable(onClick = onWhereTapped)
            .padding(horizontal = Space.sm, vertical = Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = if (state.location == null) {
                "Add where you are"
            } else {
                "Update where you are"
            },
            tint = if (state.location != null) MaterialTheme.colorScheme.primary else diva.muted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            color = if (state.location != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                diva.muted
            },
        )
    }
}

/**
 * Consent, in the order the user can actually give it: first whether they want the app
 * reading position at all, then why the OS is about to ask. The system dialog cannot
 * explain itself, so [LocationPrompt.RATIONALE] runs before it rather than after a denial.
 */
@Composable
private fun LocationPromptDialog(
    prompt: LocationPrompt?,
    onModeChosen: (LocationCaptureMode) -> Unit,
    onRationaleAccepted: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        null -> return

        LocationPrompt.CHOICE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Remember where you spend?") },
            text = {
                Text(
                    "Tagging an expense with its place lets it show up on your spending " +
                        "map and lets the app suggest shops you've been to before. " +
                        "Capture it on every expense, or only when you tap the place " +
                        "line? You can change this later in Settings.",
                )
            },
            confirmButton = {
                TextButton(onClick = { onModeChosen(LocationCaptureMode.ALWAYS) }) {
                    Text("Every time")
                }
            },
            dismissButton = {
                TextButton(onClick = { onModeChosen(LocationCaptureMode.ON_TAP) }) {
                    Text("Only when I tap")
                }
            },
        )

        LocationPrompt.RATIONALE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Location permission") },
            text = {
                Text(
                    "DivaFinance reads your position only to name the place you're " +
                        "spending. It's stored with the expense on this device, never " +
                        "uploaded, and the entry saves fine without it.",
                )
            },
            confirmButton = {
                TextButton(onClick = onRationaleAccepted) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Not now") }
            },
        )
    }
}

/**
 * Cash plus every card, as one segmented control at the top rather than buried in
 * details — which card paid for something is a decision made at the moment of spending,
 * not an afterthought.
 */
@Composable
private fun AccountSelector(state: QuickAddUiState, onCardChange: (String?) -> Unit) {
    // Income is not paid *with* anything, and with no cards there is nothing to choose.
    if (state.cards.isEmpty() || state.type != TransactionType.DEBIT) return

    val options = listOf<String?>(null) + state.cards.map { it.id }
    val labels = listOf("Cash") + state.cards.map { card ->
        card.lastFour?.let { "${card.name} ·$it" } ?: card.name
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
    ) {
        SegmentedControl(
            options = labels,
            selectedIndex = options.indexOf(state.selectedCardId).coerceAtLeast(0),
            onSelect = { onCardChange(options[it]) },
        )
    }
}

/** Category as tinted tiles rather than text chips — the colour is the encoding. */
@Composable
private fun CategoryPickerRow(
    state: QuickAddUiState,
    onCategoryChange: (SpendingCategory) -> Unit,
    onToggleAllCategories: () -> Unit,
) {
    val shown = if (state.showAllCategories) {
        SpendingCategory.entries.toList()
    } else {
        (state.suggestedCategories + state.category).distinct()
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        shown.forEach { category ->
            CategoryPickerItem(
                category = category,
                selected = category == state.category,
                onClick = { onCategoryChange(category) },
            )
        }
        Box(Modifier.align(Alignment.CenterVertically)) {
            CategoryChip(
                label = if (state.showAllCategories) "Less" else "More",
                onClick = onToggleAllCategories,
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

/** Common tip rates, so the usual case is one tap. */
private val TIP_PRESETS = listOf(0.0, 10.0, 12.5, 15.0, 18.0, 20.0)

/**
 * Splitting a bill. The keypad amount is the subtotal from the receipt; the tip is added
 * on top, and the whole lot is divided.
 */
@Composable
private fun SplitSection(
    state: QuickAddUiState,
    onSplitToggled: (Boolean) -> Unit,
    onTipPercentChange: (Double) -> Unit,
    onAddSplitPerson: (String) -> Unit,
    onRemoveSplitPerson: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Split this bill", style = MaterialTheme.typography.labelLarge)
        Switch(checked = state.splitEnabled, onCheckedChange = onSplitToggled)
    }

    if (!state.splitEnabled) return

    Text("Tip", style = MaterialTheme.typography.labelMedium)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        TIP_PRESETS.forEach { percent ->
            CategoryChip(
                label = if (percent == 0.0) "No tip" else "${percent.toFixed(if (percent % 1.0 == 0.0) 0 else 1)}%",
                selected = state.tipPercent == percent,
                onClick = { onTipPercentChange(percent) },
            )
        }
    }

    Text("Split with", style = MaterialTheme.typography.labelMedium)

    // People already on this bill; tapping one takes them off again.
    if (state.splitWith.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            state.splitWith.forEach { person ->
                CategoryChip(
                    label = "${person.name}  ×",
                    selected = true,
                    onClick = { onRemoveSplitPerson(person.name) },
                )
            }
        }
    }

    // Everyone known who is not already on the bill.
    val available = state.peopleSuggestions.filterNot { known ->
        state.splitWith.any { it.name.equals(known.name, ignoreCase = true) }
    }
    if (available.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            available.forEach { person ->
                CategoryChip(label = person.name, onClick = { onAddSplitPerson(person.name) })
            }
        }
    }

    NewSplitPersonField(onAdd = onAddSplitPerson)

    SplitBreakdown(state)
}

/** Adds someone who is not in the list yet. */
@Composable
private fun NewSplitPersonField(onAdd: (String) -> Unit) {
    var typed by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivaTextField(
            value = typed,
            onValueChange = { typed = it },
            label = "Add someone",
            modifier = Modifier.weight(1f),
        )
        CategoryChip(
            label = "Add",
            selected = typed.isNotBlank(),
            onClick = {
                onAdd(typed)
                typed = ""
            },
        )
    }
}

/** The numbers, so the division is checkable before it is saved. */
@Composable
private fun SplitBreakdown(state: QuickAddUiState) {
    val split = state.split
    if (split == null) {
        Text(
            text = "Enter an amount to split.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DivaCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (split.tipMinor > 0L) {
                BreakdownRow("Tip", split.tipMinor.toMajorUnits())
            }
            BreakdownRow("Total charged", split.totalMinor.toMajorUnits(), emphasise = true)
            BreakdownRow("Your share", split.payerShareMinor.toMajorUnits(), emphasise = true)

            split.shares.drop(1).forEach { share ->
                BreakdownRow(share.participant.name, share.amountMinor.toMajorUnits())
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, emphasise: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (emphasise) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
        )
        Text(
            text = amount.toFixed(2),
            style = if (emphasise) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
        )
    }
}

/**
 * Everything that only makes sense once there is a fix. There is no switch here any more:
 * the place line under the amount is the single entry point, so this section is the tail
 * of that interaction rather than a control of its own — and it stays empty, taking no
 * height at all, until location has actually been used.
 */
@Composable
private fun LocationSection(
    state: QuickAddUiState,
    onLocationNameChange: (String) -> Unit,
    onNearbyPlacePicked: (NearbyPlace) -> Unit,
) {
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
}

@Preview
@Composable
private fun AddExpenseEmptyPreview() {
    DivaTheme {
        AddExpenseContent(state = QuickAddUiState())
    }
}

@Preview
@Composable
private fun AddExpenseFilledPreview() {
    DivaTheme {
        AddExpenseContent(
            state = QuickAddUiState(
                expression = "42.50",
                merchantName = "Diva Coffee",
                category = SpendingCategory.DINING,
                showDetails = true,
                note = "Morning routine"
            )
        )
    }
}

@Preview
@Composable
private fun AddExpenseDarkPreview() {
    DivaTheme(darkTheme = true) {
        AddExpenseContent(
            state = QuickAddUiState(
                expression = "99.99+1",
                merchantName = "Tech Store",
                category = SpendingCategory.SHOPPING,
                showDetails = false
            )
        )
    }
}
