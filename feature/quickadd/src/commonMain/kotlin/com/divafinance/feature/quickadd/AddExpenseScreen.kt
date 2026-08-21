package com.divafinance.feature.quickadd

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divafinance.core.common.toFixed
import com.divafinance.core.common.toMajorUnits
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.enums.LocationCaptureMode
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.adaptive.DivaScaffoldColumn
import com.divafinance.core.ui.adaptive.DivaSwitch
import com.divafinance.core.ui.component.CalculatorKeypad
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.CategoryPickerItem
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.NumericStyle
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

    DivaScaffoldColumn(
        title = "New expense",
        modifier = modifier,
        onBack = {
            viewModel.reset()
            onDismiss()
        },
        backLabel = "Cancel",
        actions = {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = "Scan a receipt instead",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp).clickable(onClick = onOpenScanner),
            )
        },
    ) { padding ->
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
            onLocationCleared = viewModel::onLocationCleared,
            onSplitToggled = viewModel::onSplitToggled,
            onSplitCountChange = viewModel::onSplitCountChange,
            onTipPercentChange = viewModel::onTipPercentChange,
            onAddSplitPerson = { name -> viewModel.onAddSplitPerson(name) },
            onRemoveSplitPerson = viewModel::onRemoveSplitPerson,
            onSave = viewModel::save,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
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
    onLocationCleared: () -> Unit = {},
    onSplitToggled: (Boolean) -> Unit = {},
    onSplitCountChange: (Int) -> Unit = {},
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
        modifier = modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
    ) {
        Column(
            // `fill = false` so the body still measures in an unbounded parent — the
            // tests render this composable on its own, without a height to divide up.
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.pad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        AmountDisplay(
            state = state,
            onWhereTapped = onWhereTapped,
            onLocationNameChange = onLocationNameChange,
            onNearbyPlacePicked = onNearbyPlacePicked,
            onLocationCleared = onLocationCleared,
        )

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

        SplitSelector(state, onSplitCountChange)

        SplitSection(
            state = state,
            onSplitToggled = onSplitToggled,
            onTipPercentChange = onTipPercentChange,
            onAddSplitPerson = onAddSplitPerson,
            onRemoveSplitPerson = onRemoveSplitPerson,
        )

            Spacer(Modifier.height(Space.sm))
        }

        // Pinned rather than scrolled to: the amount is entered on the keypad above it,
        // so the commit has to stay reachable without scrolling back down.
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Space.pad, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
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
        }
    }
}

/**
 * Shows the raw expression above its running total, so "12+8" stays visible and readable
 * while the total updates underneath.
 */
@Composable
private fun AmountDisplay(
    state: QuickAddUiState,
    onWhereTapped: () -> Unit,
    onLocationNameChange: (String) -> Unit,
    onNearbyPlacePicked: (NearbyPlace) -> Unit,
    onLocationCleared: () -> Unit,
) {
    val amount = state.previewAmount
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Space.sm, bottom = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatCurrency(amount ?: 0.0),
            // NumericStyle, not a display slot: the figure changes a digit at a time as
            // the keypad is used, and tabular figures stop it jittering sideways.
            style = NumericStyle.copy(
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
            ),
            // Greyed until there is a real figure, so the zero reads as a placeholder
            // rather than as an amount someone might save by accident.
            color = if (amount == null || amount == 0.0) {
                diva.muted.copy(alpha = 0.6f)
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
            WhereLine(
                state = state,
                onWhereTapped = onWhereTapped,
                onLocationNameChange = onLocationNameChange,
                onNearbyPlacePicked = onNearbyPlacePicked,
                onLocationCleared = onLocationCleared,
            )
        }
    }
}

/**
 * The caption under the amount, and the whole of the location UI's entry point.
 *
 * One control does every location job there is, choosing by what has already been settled
 * — opt in, grant, capture, then name — because to the user they are all "tell the app
 * where I am". It reads as a label until tapped, which is why it carries a pin and a
 * spelled-out content description: nothing else marks it as interactive.
 *
 * Once there is a fix the tap opens [PlaceDialog] instead of silently re-reading the
 * position: with coordinates already in hand the open question is what the place is
 * called, and the nearby shops from the user's own history are the best answers to it.
 * Re-reading moves inside that dialog, where it is one button among the alternatives.
 *
 * Holding the line gives the two things a tap cannot: the same editor, named as such, and
 * removal. Removing has no other home — it is rare, and it is the one location action that
 * throws something away, so a menu that has to be asked for is the right weight for it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhereLine(
    state: QuickAddUiState,
    onWhereTapped: () -> Unit,
    onLocationNameChange: (String) -> Unit,
    onNearbyPlacePicked: (NearbyPlace) -> Unit,
    onLocationCleared: () -> Unit,
) {
    var naming by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val place = state.locationName.ifBlank { state.merchantName }
    val label = when {
        state.isLocatingNow -> "Finding you…"
        place.isNotBlank() -> place
        // Worth saying rather than falling back to "Where?", which would read as though
        // nothing had been tried yet.
        state.locationUnavailable -> "Location unavailable"
        else -> "Where?"
    }

    if (naming) {
        PlaceDialog(
            state = state,
            onLocationNameChange = onLocationNameChange,
            onNearbyPlacePicked = {
                naming = false
                onNearbyPlacePicked(it)
            },
            onFindAgain = onWhereTapped,
            onDismiss = { naming = false },
        )
    }

    // The menu anchors to the line, so it has to share a Box with it.
    Box {
        Row(
            modifier = Modifier
                .clip(Pill)
                .combinedClickable(
                    // Nothing to edit or remove until there is a fix, so a hold with none
                    // stays silent rather than opening a menu of two dead options.
                    onLongClick = { if (state.location != null) menuOpen = true },
                    onClick = { if (state.location == null) onWhereTapped() else naming = true },
                )
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

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Edit place") },
                onClick = {
                    menuOpen = false
                    naming = true
                },
            )
            DropdownMenuItem(
                text = { Text("Remove place") },
                onClick = {
                    menuOpen = false
                    onLocationCleared()
                },
            )
        }
    }
}

/**
 * Naming the place, once there is a fix to name.
 *
 * The nearby list is the point of it: shops from the user's own history within reach of
 * this position, which is both a faster answer than typing and a better one — picking one
 * fills the merchant in too and sharpens the category prediction. The geocoder's guess is
 * only a starting point, so the field stays editable underneath, and re-reading the
 * position is a button here rather than a second gesture on the line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceDialog(
    state: QuickAddUiState,
    onLocationNameChange: (String) -> Unit,
    onNearbyPlacePicked: (NearbyPlace) -> Unit,
    onFindAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Where are you?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (state.nearbyPlaces.isNotEmpty()) {
                    Text("Nearby", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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

                DivaTextField(
                    value = state.locationName,
                    onValueChange = onLocationNameChange,
                    label = "Place",
                )

                if (state.locationUnavailable) {
                    Text(
                        text = "Couldn't read your position just now. The entry saves fine " +
                            "without it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            // Stays open on purpose: the refreshed fix brings a new nearby list with it,
            // and that list is what the user came here for.
            TextButton(onClick = onFindAgain, enabled = !state.isLocatingNow) {
                Text(if (state.isLocatingNow) "Finding…" else "Find me again")
            }
        },
    )
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

/** The counts offered without asking. Anything else is a long press away. */
private val SPLIT_PRESETS = listOf(0, 1, 2, 3)

/** Everything the long-press sheet offers, since a table of twelve is still a real bill. */
private val SPLIT_CHOICES = (0..12).toList()

private fun splitLabel(others: Int): String =
    if (others == 0) "Just me" else "Split with $others"

/**
 * How many ways the bill goes, in the same segmented control the card selector uses —
 * because "who is paying" and "how many are sharing" are the same kind of decision, made
 * at the same moment, and neither is worth a trip into the details section.
 *
 * The visible segments cover the common table sizes; holding any of them opens the full
 * range, so a larger group costs one extra gesture instead of a permanently wider control.
 * Picking a number here needs no names: it divides the bill and keeps the other shares out
 * of the user's own spending. Naming people, in the split section below, is the separate
 * choice to also track what they owe.
 */
@Composable
private fun SplitSelector(state: QuickAddUiState, onSplitCountChange: (Int) -> Unit) {
    // Income arrives whole; there is nobody to share it with.
    if (state.type != TransactionType.DEBIT) return

    var picking by remember { mutableStateOf(false) }
    val selected = state.splitOthers
    // A count chosen from the dialog joins the presets, so it stays visible and selected.
    val options = (SPLIT_PRESETS + selected).distinct().sorted()

    // Wrapped so the dialog's zero-size node stays inside this composable: emitted a level
    // up it would land in a `spacedBy` column and open a 12dp gap while the dialog is up.
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
        ) {
            SegmentedControl(
                options = options.map(::splitLabel),
                selectedIndex = options.indexOf(selected).coerceAtLeast(0),
                onLongPress = { picking = true },
                onSelect = { onSplitCountChange(options[it]) },
            )
        }

        if (picking) {
            SplitCountDialog(
                selected = selected,
                onPick = {
                    picking = false
                    onSplitCountChange(it)
                },
                onDismiss = { picking = false },
            )
        }
    }
}

/** The full range, reached by holding the selector. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SplitCountDialog(selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split with how many?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(
                    "The bill is divided evenly between you and everyone you count here. " +
                        "Only your own share reaches your spending totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Wraps rather than scrolls: a number hidden off the edge of a dialog is
                // a number nobody finds.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SPLIT_CHOICES.forEach { count ->
                        CategoryChip(
                            label = if (count == 0) "Just me" else count.toString(),
                            selected = count == selected,
                            onClick = { onPick(count) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
        DivaSwitch(checked = state.splitEnabled, onCheckedChange = onSplitToggled)
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
