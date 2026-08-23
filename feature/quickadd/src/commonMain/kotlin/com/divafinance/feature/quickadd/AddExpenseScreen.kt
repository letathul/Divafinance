package com.divafinance.feature.quickadd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divafinance.core.common.toFixed
import com.divafinance.core.common.toMajorUnits
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.Currency
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.adaptive.DivaSwitch
import com.divafinance.core.ui.component.Avatar
import com.divafinance.core.ui.component.CalculatorKeypad
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.component.color
import com.divafinance.core.ui.component.icon
import com.divafinance.core.ui.component.initialsOf
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency
import com.divafinance.core.ui.util.formatShortDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

/*
 * Screen geometry, taken from the design rather than from `Space`: this screen is a card
 * floating on a gradient, not a run of rows on the canvas, so it has an inset of its own.
 */
private val ScreenGutter = 20.dp
private val CardInset = 22.dp
private val CardRadius = 28.dp
private val BlockRadius = 14.dp
private val FieldRadius = 12.dp

/**
 * Full-screen flow for logging a transaction in as few taps as possible: tap the amount,
 * type it on the calculator, accept the pre-selected category, save.
 *
 * Everything beyond the amount has a usable default, so the common path never requires
 * the soft keyboard. Note, location, split and receipt sit behind four disclosure chips,
 * and each one reveals its block in place rather than pushing a screen.
 *
 * **This screen draws its own chrome** rather than taking `DivaScaffold`, and is the third
 * deliberate exception to that rule alongside `OnboardingScreen` and `MapFallbackScreen`.
 * It is a full-bleed gradient with a floating card, opened from the centre tab-bar button
 * and outside the tab shell entirely; a nav bar over it would be chrome for a screen that
 * has exactly one way out.
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

    AddExpenseContent(
        state = state,
        modifier = modifier,
        onDismiss = {
            viewModel.reset()
            onDismiss()
        },
        onDigit = viewModel::onDigit,
        onOperator = viewModel::onOperator,
        onGroup = viewModel::onGroup,
        onBackspace = viewModel::onBackspace,
        onClear = viewModel::onClear,
        onEquals = viewModel::onEquals,
        onToggleCalculator = viewModel::onToggleCalculator,
        onCalculatorDismissed = viewModel::onCalculatorDismissed,
        onToggleCurrencyPicker = viewModel::onToggleCurrencyPicker,
        onCurrencyChange = viewModel::onCurrencyChange,
        onTypeChange = viewModel::onTypeChange,
        onCategoryChange = viewModel::onCategoryChange,
        onToggleAllCategories = viewModel::onToggleAllCategories,
        onToggleDetails = viewModel::onToggleDetails,
        onMerchantChange = viewModel::onMerchantChange,
        onMerchantSuggestionPicked = viewModel::onMerchantSuggestionPicked,
        onNoteChange = viewModel::onNoteChange,
        onCardChange = viewModel::onCardChange,
        onDayChange = viewModel::onDayChange,
        onOpenScanner = onOpenScanner,
        onLocationChipToggled = viewModel::onLocationChipToggled,
        onLocationAllowed = viewModel::onLocationAllowed,
        onLocationDeclined = viewModel::onLocationDeclined,
        onAutoCaptureChanged = viewModel::onAutoCaptureChanged,
        onFindMeAgain = viewModel::onWhereTapped,
        onLocationNameChange = viewModel::onLocationNameChange,
        onNearbyPlacePicked = viewModel::onNearbyPlacePicked,
        onLocationCleared = viewModel::onLocationCleared,
        onSplitToggled = viewModel::onSplitToggled,
        onSplitCountChange = viewModel::onSplitCountChange,
        onTipPercentChange = viewModel::onTipPercentChange,
        onAddSplitPerson = { name -> viewModel.onAddSplitPerson(name) },
        onRemoveSplitPerson = viewModel::onRemoveSplitPerson,
        onSave = viewModel::save,
    )
}

/**
 * Stateless body, split out so it can be driven directly from tests and previews without
 * a ViewModel or a navigation host. Callbacks default to no-ops for exactly that reason;
 * the real call site above passes every one.
 */
@Composable
internal fun AddExpenseContent(
    state: QuickAddUiState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onDigit: (Char) -> Unit = {},
    onOperator: (Char) -> Unit = {},
    onGroup: (Char) -> Unit = {},
    onBackspace: () -> Unit = {},
    onClear: () -> Unit = {},
    onEquals: () -> Unit = {},
    onToggleCalculator: () -> Unit = {},
    onCalculatorDismissed: () -> Unit = {},
    onToggleCurrencyPicker: () -> Unit = {},
    onCurrencyChange: (String) -> Unit = {},
    onTypeChange: (TransactionType) -> Unit = {},
    onCategoryChange: (SpendingCategory) -> Unit = {},
    onToggleAllCategories: () -> Unit = {},
    onToggleDetails: () -> Unit = {},
    onMerchantChange: (String) -> Unit = {},
    onMerchantSuggestionPicked: (String) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onCardChange: (String?) -> Unit = {},
    onDayChange: (QuickAddDay) -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onLocationChipToggled: () -> Unit = {},
    onLocationAllowed: () -> Unit = {},
    onLocationDeclined: () -> Unit = {},
    onAutoCaptureChanged: (Boolean) -> Unit = {},
    onFindMeAgain: () -> Unit = {},
    onLocationNameChange: (String) -> Unit = {},
    onNearbyPlacePicked: (NearbyPlace) -> Unit = {},
    onLocationCleared: () -> Unit = {},
    onSplitToggled: (Boolean) -> Unit = {},
    onSplitCountChange: (Int) -> Unit = {},
    onTipPercentChange: (Double) -> Unit = {},
    onAddSplitPerson: (String) -> Unit = {},
    onRemoveSplitPerson: (String) -> Unit = {},
    onSave: () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(addExpenseGradient())) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = ScreenGutter)
                .padding(top = Space.md, bottom = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AddExpenseHeader(onDismiss)

            DayChip(state.day, onDayChange)

            // The card takes the rest of the screen so the save action sits at the
            // bottom of it rather than wherever the content happens to end.
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(CardRadius),
                color = MaterialTheme.colorScheme.surface,
                // The one place this design system uses elevation: the card floats over a
                // saturated gradient rather than sitting on the canvas, and a hairline
                // has nothing to separate it from there.
                shadowElevation = 16.dp,
            ) {
                Column(Modifier.padding(CardInset).imePadding().navigationBarsPadding()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        AmountBlock(
                            state = state,
                            onToggleCalculator = onToggleCalculator,
                            onToggleCurrencyPicker = onToggleCurrencyPicker,
                            onCurrencyChange = onCurrencyChange,
                            onTypeChange = onTypeChange,
                            onCardChange = onCardChange
                        )

                        CategoryBlock(state, onCategoryChange, onToggleAllCategories)

                        CardDivider()

                        DetailChips(
                            state = state,
                            onToggleDetails = onToggleDetails,
                            onLocationChipToggled = onLocationChipToggled,
                            onSplitToggled = onSplitToggled,
                            onOpenScanner = onOpenScanner,
                        )

                        NoteBlock(
                            state = state,
                            onMerchantChange = onMerchantChange,
                            onMerchantSuggestionPicked = onMerchantSuggestionPicked,
                            onNoteChange = onNoteChange,
                        )

                        LocationBlock(
                            state = state,
                            onLocationAllowed = onLocationAllowed,
                            onLocationDeclined = onLocationDeclined,
                            onAutoCaptureChanged = onAutoCaptureChanged,
                            onFindMeAgain = onFindMeAgain,
                            onLocationNameChange = onLocationNameChange,
                            onNearbyPlacePicked = onNearbyPlacePicked,
                            onLocationCleared = onLocationCleared,
                        )

                        SplitBlock(
                            state = state,
                            onSplitCountChange = onSplitCountChange,
                            onTipPercentChange = onTipPercentChange,
                            onAddSplitPerson = onAddSplitPerson,
                            onRemoveSplitPerson = onRemoveSplitPerson,
                        )
                    }

                    // Pinned rather than scrolled to: the amount is the only required
                    // field, so the commit stays reachable from the moment it is entered.
                    Spacer(Modifier.height(Space.md))
                    state.error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = Space.sm),
                        )
                    }
                    DivaButton(
                        text = if (state.isSaving) "Saving…" else "Save Transaction",
                        onClick = onSave,
                        enabled = state.canSave,
                    )
                }
            }
        }

        AmountSheet(
            state = state,
            onDigit = onDigit,
            onOperator = onOperator,
            onGroup = onGroup,
            onBackspace = onBackspace,
            onClear = onClear,
            onEquals = onEquals,
            onConfirm = onCalculatorDismissed,
        )
    }
}

/**
 * Violet to pale sky, built out of the user's accent rather than fixed: this screen is
 * the one full-bleed colour surface in the app, and it would sit oddly against every
 * other screen if it stayed purple while the accent was green.
 */
@Composable
private fun addExpenseGradient(): Brush {
    val accent = diva.accent
    val stops = if (diva.isDark) {
        listOf(
            lerp(accent, Color.Black, 0.55f),
            lerp(accent, Color.Black, 0.78f),
            MaterialTheme.colorScheme.background,
        )
    } else {
        listOf(
            accent,
            lerp(accent, Color(0xFF8FA0F2), 0.62f),
            Color(0xFFC3DDF7),
        )
    }
    return Brush.linearGradient(stops)
}

/** Close, title, and nothing else — there is one way out of this screen. */
@Composable
private fun AddExpenseHeader(onDismiss: () -> Unit) {
    // The gutter comes from the screen's outer Column, which applies it to the DayChip and
    // the card too — they used to sit flush against the screen edge while this did not.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (diva.isDark) 0.14f else 0.35f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Cancel",
                tint = onGradient(),
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = "Add Transaction",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = onGradient(),
        )
        Spacer(Modifier.width(32.dp))
    }
}

/** Readable on the gradient in either scheme, which `onSurface` is not. */
@Composable
private fun onGradient(): Color = if (diva.isDark) Color.White else Color(0xFF1A1A1A)

/**
 * Which day the entry is booked against, as a pill over the gradient.
 *
 * Only today and yesterday: backdating further is the full form's job, and a date picker
 * here would be a modal in front of a modal for a case this screen is not for.
 */
@Composable
private fun DayChip(day: QuickAddDay, onDayChange: (QuickAddDay) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(Pill)
                .background(Color.White.copy(alpha = if (diva.isDark) 0.14f else 0.5f))
                .clickable { open = true }
                .padding(horizontal = 13.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = onGradient(),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "${day.label}, ${formatShortDate(day.dateFrom(today))}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = onGradient(),
            )
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = "Change the day",
                tint = onGradient().copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            QuickAddDay.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.label}, ${formatShortDate(option.dateFrom(today))}") },
                    onClick = {
                        open = false
                        onDayChange(option)
                    },
                )
            }
        }
    }
}

/** Mirrors `QuickAddViewModel.dateFor`, for the label only. */
private fun QuickAddDay.dateFrom(today: LocalDate): LocalDate = when (this) {
    QuickAddDay.TODAY -> today
    QuickAddDay.YESTERDAY -> LocalDate.fromEpochDays(today.toEpochDays() - 1)
}

/**
 * The figure, and what it is denominated in.
 *
 * Tapping the figure is the only way to the keypad. Putting the pad behind the amount
 * rather than under it is what lets the rest of the form be read in one screen — and the
 * amount is where a user's finger already is when they open this screen.
 */
@Composable
private fun AmountBlock(
    state: QuickAddUiState,
    onToggleCalculator: () -> Unit,
    onToggleCurrencyPicker: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit = {},
    onCardChange: (String?) -> Unit,
) {
    val amount = state.previewAmount
    Column(
        modifier = Modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            text = formatCurrency(amount ?: 0.0, state.currency),
            // NumericStyle, not a display slot: the figure changes a digit at a time as
            // the keypad is used, and tabular figures stop it jittering sideways.
            style = NumericStyle.copy(
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
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
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onToggleCalculator)
                .semantics { contentDescription = "Edit the amount" }
                .padding(horizontal = Space.sm),
        )

        // Only worth showing once it is an actual sum rather than a repeat of the total.
        if (state.expression.any { it in "+-*/()" }) {
            Text(
                text = state.expression,
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            CurrencyPill(state, onToggleCurrencyPicker)
            TypeAndAccountRow(state, onTypeChange, onCardChange)
        }

        if (state.currencyPickerOpen) {
            CurrencyList(state.currency, onCurrencyChange)
        }
    }
}

@Composable
private fun CurrencyPill(state: QuickAddUiState, onToggle: () -> Unit) {
    val open = state.currencyPickerOpen
    val currency = state.currencyInfo
    Row(
        modifier = Modifier
            .wrapContentSize()
            .clip(Pill)
            .background(if (open) diva.accent.copy(alpha = 0.12f) else diva.keyFill.copy(alpha = 0.4f))
            .border(1.dp, if (open) diva.accent.copy(alpha = 0.4f) else Color.Transparent, Pill)
            .clickable(onClick = onToggle)
            .padding(horizontal = Space.pad, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${currency.symbol}  ${currency.code} · ${currency.name}",
            style = MaterialTheme.typography.labelLarge,
            color = if (open) diva.accent else MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = "Change currency",
            tint = diva.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CurrencyList(selected: String, onCurrencyChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BlockRadius))
            .background(diva.barFill)
            .heightIn(max = 176.dp)
            .verticalScroll(rememberScrollState())
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Currency.supported.forEach { currency ->
            val on = currency.code == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) diva.accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (on) diva.accent.copy(alpha = 0.4f) else diva.fgHair,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onCurrencyChange(currency.code) }
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (on) diva.accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(38.dp),
                )
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = diva.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = diva.muted,
                )
            }
        }
    }
}

/**
 * Expense or income, and what paid for it.
 *
 * Which card paid is a decision made at the moment of spending, not an afterthought, so
 * it stays above the fold rather than going behind a chip — but income is not paid *with*
 * anything, which is why the account selector disappears with the type.
 */
@Composable
private fun TypeAndAccountRow(
    state: QuickAddUiState,
    onTypeChange: (TransactionType) -> Unit,
    onCardChange: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentedControl(
            options = listOf("Expense", "Income"),
            selectedIndex = if (state.type == TransactionType.CREDIT) 1 else 0,
            onSelect = {
                onTypeChange(if (it == 1) TransactionType.CREDIT else TransactionType.DEBIT)
            },
        )

        if (state.cards.isNotEmpty() && state.type == TransactionType.DEBIT) {
            val options = listOf<String?>(null) + state.cards.map { it.id }
            val labels = listOf("Cash") + state.cards.map { card ->
                card.lastFour?.let { "${card.name} ·$it" } ?: card.name
            }
            SegmentedControl(
                options = labels,
                selectedIndex = options.indexOf(state.selectedCardId).coerceAtLeast(0),
                onSelect = { onCardChange(options[it]) },
            )
        }
    }
}

/**
 * The predicted categories as tinted pills, with the rest one tap behind `+ New`.
 *
 * The full list is twelve — long enough that showing it all would bury the details below
 * it, short enough that a search field would be silly. Prediction is what makes the first
 * few right most of the time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryBlock(
    state: QuickAddUiState,
    onCategoryChange: (SpendingCategory) -> Unit,
    onToggleAllCategories: () -> Unit,
) {
    val shown = (state.suggestedCategories + state.category).distinct()

    FieldLabel("Category")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        shown.forEach { category ->
            CategoryPill(
                category = category,
                selected = category == state.category,
                onClick = { onCategoryChange(category) },
            )
        }
        DashedPill(
            label = "+ New",
            active = state.showAllCategories,
            onClick = onToggleAllCategories,
        )
    }

    if (state.showAllCategories) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BlockRadius))
                .background(diva.barFill)
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FieldLabel("More categories")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SpendingCategory.entries.filterNot { it in shown }.forEach { category ->
                    CategoryPill(
                        category = category,
                        selected = false,
                        onClick = { onCategoryChange(category) },
                    )
                }
            }
        }
    }
}

/** Category colour and glyph on a pill — the same encoding the ledger rows use. */
@Composable
private fun CategoryPill(
    category: SpendingCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = category.color
    Row(
        modifier = Modifier
            .clip(Pill)
            .background(if (selected) tint.copy(alpha = 0.14f) else diva.keyFill.copy(alpha = 0.35f))
            .border(1.dp, if (selected) tint else Color.Transparent, Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            category.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else diva.muted,
        )
    }
}

/** The four things an entry can carry beyond an amount, as a 2×2 of disclosure chips. */
@Composable
private fun DetailChips(
    state: QuickAddUiState,
    onToggleDetails: () -> Unit,
    onLocationChipToggled: () -> Unit,
    onSplitToggled: (Boolean) -> Unit,
    onOpenScanner: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        FieldLabel("Add details")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            DetailChip(
                label = "Note & tags",
                icon = Icons.Outlined.Edit,
                active = state.showDetails,
                onClick = onToggleDetails,
                modifier = Modifier.weight(1f),
            )
            DetailChip(
                label = "Location",
                icon = Icons.Outlined.LocationOn,
                active = state.showLocation,
                onClick = onLocationChipToggled,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            DetailChip(
                label = "Split",
                icon = Icons.Outlined.People,
                active = state.splitEnabled,
                // Income arrives whole; there is nobody to share it with.
                enabled = state.type == TransactionType.DEBIT,
                onClick = { onSplitToggled(!state.splitEnabled) },
                modifier = Modifier.weight(1f),
            )
            DetailChip(
                label = "Receipt",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                active = false,
                onClick = onOpenScanner,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(FieldRadius)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (active) diva.accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (active) diva.accent.copy(alpha = 0.4f) else diva.fgHair, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.md, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = when {
                !enabled -> diva.muted.copy(alpha = 0.4f)
                active -> diva.accent
                else -> diva.muted
            },
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                !enabled -> diva.muted.copy(alpha = 0.4f)
                active -> diva.accent
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** Merchant and note, behind the first chip. */
@Composable
private fun NoteBlock(
    state: QuickAddUiState,
    onMerchantChange: (String) -> Unit,
    onMerchantSuggestionPicked: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    if (!state.showDetails) return

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        DivaTextField(
            value = state.merchantName,
            onValueChange = onMerchantChange,
            label = "Merchant",
        )

        // Shops repeat, so offering past ones saves most of the typing — and picking one
        // sharpens the category prediction at the same time.
        if (state.merchantSuggestions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
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

        DivaTextField(value = state.note, onValueChange = onNoteChange, label = "Note")
    }
}

/**
 * Location, behind the second chip, and the whole of the location UI there is.
 *
 * Consent is one card rather than two dialogs: the card *is* the rationale the system
 * prompt cannot give, so accepting it opts in and launches that prompt in one move.
 * Once there is a fix the block becomes the place itself, with the nearby shops from the
 * user's own history as the fastest way to name it — picking one fills in the merchant
 * too, and sharpens the category prediction.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationBlock(
    state: QuickAddUiState,
    onLocationAllowed: () -> Unit,
    onLocationDeclined: () -> Unit,
    onAutoCaptureChanged: (Boolean) -> Unit,
    onFindMeAgain: () -> Unit,
    onLocationNameChange: (String) -> Unit,
    onNearbyPlacePicked: (NearbyPlace) -> Unit,
    onLocationCleared: () -> Unit,
) {
    if (!state.showLocation) return

    if (state.locationNeedsConsent || state.locationPrompt != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FieldRadius))
                .background(diva.barFill)
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = "Use your location on this entry?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "It names the place you're spending, so the entry can show up on " +
                    "your spending map. It stays on this device, and the entry saves " +
                    "fine without it.",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                PillButton(
                    label = "Not now",
                    onClick = onLocationDeclined,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    label = "Allow",
                    onClick = onLocationAllowed,
                    filled = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(diva.barFill)
                .padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (state.location != null) diva.accent else diva.muted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = when {
                        state.isLocatingNow -> "Finding you…"
                        state.locationName.isNotBlank() -> state.locationName
                        state.locationUnavailable -> "Location unavailable"
                        else -> "Where are you?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelSmall,
                color = diva.muted,
                modifier = Modifier
                    .clip(Pill)
                    .clickable(onClick = onLocationCleared)
                    .padding(horizontal = Space.sm, vertical = Space.xs),
            )
        }

        // Shops from the user's own history within reach of this fix: both a faster
        // answer than typing and a better one.
        if (state.nearbyPlaces.isNotEmpty()) {
            FieldLabel("Nearby")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
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

        // The geocoder's guess is a starting point, not a fact.
        if (state.location != null) {
            DivaTextField(
                value = state.locationName,
                onValueChange = onLocationNameChange,
                label = "Place",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Capture location next time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DivaSwitch(
                checked = state.autoCaptureLocation,
                onCheckedChange = onAutoCaptureChanged,
            )
        }

        TextButton(onClick = onFindMeAgain, enabled = !state.isLocatingNow) {
            Text(if (state.isLocatingNow) "Finding…" else "Find me again")
        }
    }
}

/**
 * Splitting a bill, behind the third chip.
 *
 * Two paths that never mix. The count is the quick one — unnamed heads owe nothing back,
 * so it only keeps their portion out of the user's own spending. Naming people is the
 * separate, slower choice to also track what they owe, and it takes over the count.
 */
@Composable
private fun SplitBlock(
    state: QuickAddUiState,
    onSplitCountChange: (Int) -> Unit,
    onTipPercentChange: (Double) -> Unit,
    onAddSplitPerson: (String) -> Unit,
    onRemoveSplitPerson: (String) -> Unit,
) {
    if (!state.splitEnabled || state.type != TransactionType.DEBIT) return

    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        FieldLabel("How many ways")
        SplitCountSelector(state, onSplitCountChange)

        FieldLabel("Who's on it")
        SplitPeopleRow(state, onAddSplitPerson, onRemoveSplitPerson)

        FieldLabel("Tip")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            TIP_PRESETS.forEach { percent ->
                CategoryChip(
                    label = if (percent == 0.0) {
                        "No tip"
                    } else {
                        "${percent.toFixed(if (percent % 1.0 == 0.0) 0 else 1)}%"
                    },
                    selected = state.tipPercent == percent,
                    onClick = { onTipPercentChange(percent) },
                )
            }
        }

        SplitBreakdown(state)
    }
}

/** The counts offered without asking. Anything else is a long press away. */
private val SPLIT_PRESETS = listOf(0, 1, 2, 3)

/** Everything the long-press dialog offers, since a table of twelve is still a real bill. */
private val SPLIT_CHOICES = (0..12).toList()

/** Common tip rates, so the usual case is one tap. */
private val TIP_PRESETS = listOf(0.0, 10.0, 12.5, 15.0, 18.0, 20.0)

private fun splitLabel(others: Int): String =
    if (others == 0) "Just me" else "Split with $others"

/**
 * How many ways the bill goes. The visible segments cover the common table sizes; holding
 * any of them opens the full range, so a larger group costs one extra gesture instead of
 * a permanently wider control.
 */
@Composable
private fun SplitCountSelector(state: QuickAddUiState, onSplitCountChange: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val selected = state.splitOthers
    // A count chosen from the dialog joins the presets, so it stays visible and selected.
    val options = (SPLIT_PRESETS + selected).distinct().sorted()

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
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

/**
 * Everyone on the bill as avatars: you first, then anyone named, then the button that
 * names another. Tapping someone already on it takes them off — the same gesture in and
 * out, since a row of four avatars has no room for four remove targets.
 */
@Composable
private fun SplitPeopleRow(
    state: QuickAddUiState,
    onAddSplitPerson: (String) -> Unit,
    onRemoveSplitPerson: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        Avatar(initials = "You", color = diva.muted, size = 37.dp)

        state.splitWith.forEach { person ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onRemoveSplitPerson(person.name) }
                    .semantics { contentDescription = "Take ${person.name} off this bill" },
            ) {
                Avatar(
                    initials = initialsOf(person.name),
                    color = personColor(person.name),
                    size = 37.dp,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(37.dp)
                .clip(CircleShape)
                .border(1.5.dp, diva.fgHair, CircleShape)
                .clickable { adding = !adding },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add someone to this bill",
                tint = diva.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (adding) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FieldRadius))
                .background(diva.barFill)
                .padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            FieldLabel("Add person")

            // Everyone known who is not already on the bill.
            state.peopleSuggestions
                .filterNot { known ->
                    state.splitWith.any { it.name.equals(known.name, ignoreCase = true) }
                }
                .forEach { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, diva.fgHair, RoundedCornerShape(10.dp))
                            .clickable {
                                adding = false
                                onAddSplitPerson(person.name)
                            }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(
                            initials = initialsOf(person.name),
                            color = personColor(person.name),
                            size = 26.dp,
                        )
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = diva.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

            NewSplitPersonField(onAdd = {
                adding = false
                onAddSplitPerson(it)
            })
        }
    }
}

/** Adds someone who is not in the list yet. */
@Composable
private fun NewSplitPersonField(onAdd: (String) -> Unit) {
    var typed by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
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

/** A stable colour per name, so the same person keeps the same avatar between entries. */
@Composable
private fun personColor(name: String): Color {
    val ramp = SpendingCategory.entries
    val index = (name.lowercase().sumOf { it.code } % ramp.size)
    return ramp[index].color
}

/** The numbers, so the division is checkable before it is saved. */
@Composable
private fun SplitBreakdown(state: QuickAddUiState) {
    val split = state.split
    if (split == null) {
        Text(
            text = "Enter an amount to split.",
            style = MaterialTheme.typography.bodySmall,
            color = diva.muted,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldRadius))
            .background(diva.barFill)
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        if (split.tipMinor > 0L) {
            BreakdownRow("Tip", split.tipMinor.toMajorUnits(), state.currency)
        }
        BreakdownRow("Total charged", split.totalMinor.toMajorUnits(), state.currency, true)
        BreakdownRow("Your share", split.payerShareMinor.toMajorUnits(), state.currency, true)

        split.shares.drop(1).forEach { share ->
            BreakdownRow(share.participant.name, share.amountMinor.toMajorUnits(), state.currency)
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: Double,
    currency: String,
    emphasise: Boolean = false,
) {
    val style = if (emphasise) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodySmall
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = style)
        Text(text = formatCurrency(amount, currency), style = style)
    }
}

/**
 * The keypad, behind the amount.
 *
 * It carries the expression and its running result side by side because the pad has `(`,
 * `)` and `=` on it: once grouping is possible, "what have I typed" and "what does it
 * come to" stop being the same question. Confirm only dismisses — the amount is already
 * live behind the sheet, so there is nothing to commit.
 */
@Composable
private fun BoxScope.AmountSheet(
    state: QuickAddUiState,
    onDigit: (Char) -> Unit,
    onOperator: (Char) -> Unit,
    onGroup: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onConfirm: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.calculatorOpen,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(220)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(onClick = onConfirm),
        )
    }

    AnimatedVisibility(
        visible = state.calculatorOpen,
        enter = slideInVertically(tween(300)) { it },
        exit = slideOutVertically(tween(280)) { it },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(diva.barFill)
                .navigationBarsPadding()
                .padding(bottom = Space.md),
        ) {
            Box(
                Modifier
                    .padding(top = 9.dp, bottom = 3.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 38.dp, height = 4.5.dp)
                    .clip(Pill)
                    .background(diva.fgHair),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter)
                    .padding(top = 6.dp, bottom = Space.md),
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    FieldLabel(state.currency)
                    Text(
                        text = state.expression.ifBlank { "0" },
                        style = NumericStyle.copy(fontSize = 15.sp),
                        color = diva.muted,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = state.committedAmount
                        ?.let { formatCurrency(it, state.currency) }
                        ?: "—",
                    style = NumericStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    color = if (state.committedAmount == null) {
                        diva.muted.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }

            CardDivider()

            CalculatorKeypad(
                onDigit = onDigit,
                onOperator = onOperator,
                onBackspace = onBackspace,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
                extended = true,
                onGroup = onGroup,
                onClear = onClear,
                onEquals = onEquals,
            )

            DivaButton(
                text = "Confirm",
                onClick = onConfirm,
                modifier = Modifier.padding(horizontal = 13.dp),
            )
        }
    }
}

// --- small shared pieces ---------------------------------------------------

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = diva.muted,
    )
}

@Composable
private fun CardDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(diva.hairline).background(diva.fgHair))
}

/** The dashed affordance for "there are more of these" — never a committed choice. */
@Composable
private fun DashedPill(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(Pill)
            .border(1.dp, if (active) diva.accent else diva.fgHair, Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) diva.accent else diva.muted,
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(Pill)
            .background(if (filled) diva.accent else diva.keyFill)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// --- previews --------------------------------------------------------------

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
                suggestedCategories = listOf(
                    SpendingCategory.DINING,
                    SpendingCategory.GROCERIES,
                    SpendingCategory.SHOPPING,
                ),
                showDetails = true,
                note = "Morning routine",
            ),
        )
    }
}

@Preview
@Composable
private fun AddExpenseCalculatorPreview() {
    DivaTheme {
        AddExpenseContent(
            state = QuickAddUiState(expression = "12*(3+4)", calculatorOpen = true),
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
            ),
        )
    }
}
