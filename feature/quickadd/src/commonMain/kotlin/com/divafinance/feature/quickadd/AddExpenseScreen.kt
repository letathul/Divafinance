package com.divafinance.feature.quickadd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divafinance.core.common.toFixed
import com.divafinance.core.common.toMajorUnits
import com.divafinance.core.domain.engine.CardRecommendation
import com.divafinance.core.domain.engine.NearbyPlace
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.Currency
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.adaptive.DivaBottomSheet
import com.divafinance.core.ui.adaptive.DivaSwitch
import com.divafinance.core.ui.component.Avatar
import com.divafinance.core.ui.component.CalculatorKeypad
import com.divafinance.core.ui.component.CategoryChip
import com.divafinance.core.ui.component.CategoryIdentity
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCalendar
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.Hairline
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.component.byId
import com.divafinance.core.ui.component.categoryIdentity
import com.divafinance.core.ui.component.color
import com.divafinance.core.ui.component.icon
import com.divafinance.core.ui.component.initialsOf
import com.divafinance.core.ui.theme.DivaTheme
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.theme.Pill
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.core.ui.util.formatCurrency
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview

/*
 * Screen geometry, taken from the design rather than from `Space`: this flow is drawn to
 * its own spec and has an inset and a corner scale of its own.
 */
private val ScreenGutter = 20.dp
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
    onOpenCategoryPicker: () -> Unit = {},
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
        onToggleDetails = viewModel::onToggleDetails,
        onMerchantChange = viewModel::onMerchantChange,
        onMerchantSuggestionPicked = viewModel::onMerchantSuggestionPicked,
        onNoteChange = viewModel::onNoteChange,
        onCardChange = viewModel::onCardChange,
        onDateSheetOpened = viewModel::onDateSheetOpened,
        onDateSheetDismissed = viewModel::onDateSheetDismissed,
        onDisplayedMonthChange = viewModel::onDisplayedMonthChange,
        onDateChange = viewModel::onDateChange,
        onCustomCategoryChange = viewModel::onCustomCategoryChange,
        onOpenCategoryPicker = onOpenCategoryPicker,
        onUseBestCard = viewModel::onUseBestCard,
        onAddTag = viewModel::onAddTag,
        onRemoveTag = viewModel::onRemoveTag,
        onLocationSheetAllowed = viewModel::onLocationSheetAllowed,
        onLocationSheetDeclined = viewModel::onLocationSheetDeclined,
        onSplitSheetOpened = viewModel::onSplitSheetOpened,
        onSplitSheetDismissed = viewModel::onSplitSheetDismissed,
        onSplitModeChange = viewModel::onSplitModeChange,
        onSplitShareChange = viewModel::onSplitShareChange,
        onSplitPayerChange = viewModel::onSplitPayerChange,
        onOpenScanner = onOpenScanner,
        onLocationChipToggled = viewModel::onLocationChipToggled,
        onLocationAllowed = viewModel::onLocationAllowed,
        onLocationDeclined = viewModel::onLocationDeclined,
        onAutoCaptureChanged = viewModel::onAutoCaptureChanged,
        onFindMeAgain = viewModel::onWhereTapped,
        onLocationNameChange = viewModel::onLocationNameChange,
        onNearbyPlacePicked = viewModel::onNearbyPlacePicked,
        onLocationCleared = viewModel::onLocationCleared,
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
    onToggleDetails: () -> Unit = {},
    onMerchantChange: (String) -> Unit = {},
    onMerchantSuggestionPicked: (String) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onCardChange: (String?) -> Unit = {},
    onDateSheetOpened: () -> Unit = {},
    onDateSheetDismissed: () -> Unit = {},
    onDisplayedMonthChange: (LocalDate) -> Unit = {},
    onDateChange: (LocalDate) -> Unit = {},
    onCustomCategoryChange: (CustomCategory) -> Unit = {},
    onOpenCategoryPicker: () -> Unit = {},
    onUseBestCard: () -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onLocationSheetAllowed: () -> Unit = {},
    onLocationSheetDeclined: () -> Unit = {},
    onSplitSheetOpened: () -> Unit = {},
    onSplitSheetDismissed: () -> Unit = {},
    onSplitModeChange: (SplitMode) -> Unit = {},
    onSplitShareChange: (Int, Double) -> Unit = { _, _ -> },
    onSplitPayerChange: (SplitPerson?) -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onLocationChipToggled: () -> Unit = {},
    onLocationAllowed: () -> Unit = {},
    onLocationDeclined: () -> Unit = {},
    onAutoCaptureChanged: (Boolean) -> Unit = {},
    onFindMeAgain: () -> Unit = {},
    onLocationNameChange: (String) -> Unit = {},
    onNearbyPlacePicked: (NearbyPlace) -> Unit = {},
    onLocationCleared: () -> Unit = {},
    onAddSplitPerson: (String) -> Unit = {},
    onRemoveSplitPerson: (String) -> Unit = {},
    onSave: () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(diva.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = ScreenGutter)
                .padding(top = Space.md, bottom = Space.md),
        ) {
            AddExpenseHeader(onDismiss)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                Spacer(Modifier.height(Space.xs))

                TypeToggle(state.type, onTypeChange)

                AmountBlock(
                    state = state,
                    onToggleCalculator = onToggleCalculator,
                    onToggleCurrencyPicker = onToggleCurrencyPicker,
                    onCurrencyChange = onCurrencyChange,
                )

                CategoryBlock(
                    state = state,
                    onCategoryChange = onCategoryChange,
                    onCustomCategoryChange = onCustomCategoryChange,
                    onOpenCategoryPicker = onOpenCategoryPicker,
                )

                BestCardChip(state, onUseBestCard)

                CardSelector(state, onCardChange)

                DatePill(state, onDateSheetOpened)

                NoteRow(
                    state = state,
                    onToggleDetails = onToggleDetails,
                    onMerchantChange = onMerchantChange,
                    onMerchantSuggestionPicked = onMerchantSuggestionPicked,
                    onNoteChange = onNoteChange,
                    onAddTag = onAddTag,
                    onRemoveTag = onRemoveTag,
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

                SplitSummary(state, onSplitSheetOpened)
            }

            // Pinned rather than scrolled to: the amount is the only required field, so
            // the commit stays reachable from the moment it is entered.
            Spacer(Modifier.height(Space.md))

            ActionRow(
                state = state,
                onOpenScanner = onOpenScanner,
                onSplitSheetOpened = onSplitSheetOpened,
                onLocationChipToggled = onLocationChipToggled,
            )

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

    if (state.dateSheetOpen) {
        DateSheet(
            state = state,
            onDismiss = onDateSheetDismissed,
            onDisplayedMonthChange = onDisplayedMonthChange,
            onDateChange = onDateChange,
        )
    }

    if (state.splitSheetOpen) {
        SplitSheet(
            state = state,
            onDismiss = onSplitSheetDismissed,
            onSplitModeChange = onSplitModeChange,
            onSplitShareChange = onSplitShareChange,
            onSplitPayerChange = onSplitPayerChange,
            onAddSplitPerson = onAddSplitPerson,
            onRemoveSplitPerson = onRemoveSplitPerson,
        )
    }

    if (state.locationSheetOpen) {
        LocationConsentSheet(
            onAllow = onLocationSheetAllowed,
            onDecline = onLocationSheetDeclined,
        )
    }
}

/** Close, title, and nothing else — there is one way out of this screen. */
@Composable
private fun AddExpenseHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Add Transaction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(32.dp))
    }
}

/**
 * Expense or income, as the first thing on the screen.
 *
 * It sits above the amount because it changes what the amount *means*, and a control that
 * reframes the field below it belongs before that field rather than after.
 */
@Composable
private fun TypeToggle(type: TransactionType, onTypeChange: (TransactionType) -> Unit) {
    val options = listOf("Expense", "Income")
    val selected = if (type == TransactionType.DEBIT) 0 else 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Pill)
            .background(diva.keyFill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(Pill)
                    .background(if (on) diva.accent else Color.Transparent)
                    .clickable {
                        onTypeChange(
                            if (index == 0) TransactionType.DEBIT else TransactionType.CREDIT
                        )
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (on) MaterialTheme.colorScheme.surface else diva.muted,
                )
            }
        }
    }
}

/**
 * The best card for this category, computed on-device from the cards and reward rates the
 * user entered themselves. No bank connection and no network call — which is the whole
 * reason it can be offered during entry rather than after it.
 *
 * Absent until a category is settled and there is a card that can actually take the
 * charge, so it never occupies a row with nothing to say.
 */
@Composable
private fun BestCardChip(state: QuickAddUiState, onUseBestCard: () -> Unit) {
    val best = state.bestCard ?: return
    if (state.type != TransactionType.DEBIT) return
    val alreadyChosen = best.card.id == state.selectedCardId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BlockRadius))
            .background(diva.accent.copy(alpha = 0.14f))
            .border(
                1.dp,
                diva.accent.copy(alpha = if (alreadyChosen) 0.55f else 0.3f),
                RoundedCornerShape(BlockRadius),
            )
            .clickable(enabled = !alreadyChosen, onClick = onUseBestCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CreditCard,
            contentDescription = null,
            tint = diva.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = bestCardLabel(best, alreadyChosen),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!alreadyChosen) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Use this card",
                tint = diva.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** "Best card: Sapphire · 3x on Dining", or the earned figure when there is no multiplier. */
@Composable
private fun bestCardLabel(best: CardRecommendation, alreadyChosen: Boolean): String {
    val lead = if (alreadyChosen) "Using" else "Best card"
    val rule = best.rule
    val detail = when {
        rule != null -> "${rule.multiplier.trimTrailingZeros()}x on ${rule.category.displayName}"
        best.estimatedRewardValue > 0.0 -> "earns ${best.estimatedRewardValue.toFixed(2)}"
        else -> "no category bonus"
    }
    return "$lead: ${best.card.name} · $detail"
}

/** 3.0 reads as "3x", 1.5 stays "1.5x" — a trailing ".0" on a multiplier is noise. */
private fun Double.trimTrailingZeros(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toFixed(2).trimEnd('0').trimEnd('.')

/**
 * Which day the entry is booked against.
 *
 * Any past day, not just the last two: the calendar behind this pill is a sheet rather
 * than a second modal over the keypad, so backdating no longer has to be the full form's
 * job.
 */
@Composable
private fun DatePill(state: QuickAddUiState, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(Pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, diva.fgHair, Pill)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = state.dateLabel(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = "Change the date",
            tint = diva.muted,
            modifier = Modifier.size(16.dp),
        )
    }
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
) {
    val amount = state.previewAmount
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            text = formatCurrency(amount ?: 0.0, state.currency),
            // NumericStyle, not a display slot: the figure changes a digit at a time as
            // the keypad is used, and tabular figures stop it jittering sideways.
            style = NumericStyle.copy(
                fontSize = 56.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
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

        // Kept on the form as well as on the pad: once the keypad closes, this is the
        // only thing that says what the total was made of, and a receipt totalled from
        // four figures is exactly the case worth being able to check afterwards.
        if (state.expression.any { it in "+-*/()" }) {
            Text(
                text = state.expression,
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
                textAlign = TextAlign.Center,
            )
        }

        // Under the figure rather than beside it: it labels the amount, and a badge to
        // one side of a centred number pulls the whole block off centre.
        CurrencyPill(state, onToggleCurrencyPicker)

        if (state.currencyPickerOpen) {
            CurrencyList(state.currency, onCurrencyChange)
        } else if (state.committedAmount == null) {
            Text(
                text = "Tap the amount to enter with the calculator",
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
                textAlign = TextAlign.Center,
            )
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
            .background(if (open) diva.accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (open) diva.accent.copy(alpha = 0.4f) else diva.fgHair, Pill)
            .clickable(onClick = onToggle)
            .padding(start = Space.pad, end = 10.dp, top = 9.dp, bottom = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
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
 * What paid for it.
 *
 * The best-card chip above recommends one, but a recommendation the user cannot overrule
 * is an instruction — so the full list stays one tap away rather than behind a detail
 * chip. Absent for income, which is not paid *with* anything, and absent when someone
 * else paid the bill, which is not a charge on any of these cards.
 */
@Composable
private fun CardSelector(state: QuickAddUiState, onCardChange: (String?) -> Unit) {
    if (state.cards.isEmpty()) return
    if (state.type != TransactionType.DEBIT) return
    if (state.paidByOther) return

    val options = listOf<String?>(null) + state.cards.map { it.id }
    val labels = listOf("Cash") + state.cards.map { card ->
        card.lastFour?.let { "${card.name} ·$it" } ?: card.name
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        SegmentedControl(
            options = labels,
            selectedIndex = options.indexOf(state.selectedCardId).coerceAtLeast(0),
            onSelect = { onCardChange(options[it]) },
        )
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
    onCustomCategoryChange: (CustomCategory) -> Unit,
    onOpenCategoryPicker: () -> Unit,
) {
    val selectedCustom = state.customCategories.byId(state.customCategoryId)

    // Three guesses plus More, per the design. The selected one is always among them even
    // when prediction did not offer it, so the chip row never disagrees with the entry.
    val suggested = state.suggestedCategories.take(SUGGESTED_CHIP_COUNT)
    val shown = if (selectedCustom == null) {
        (listOf(state.category) + suggested).distinct().take(SUGGESTED_CHIP_COUNT)
    } else {
        suggested.take(SUGGESTED_CHIP_COUNT - 1)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        FieldLabel("Category")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            selectedCustom?.let { custom ->
                CategoryPill(
                    identity = categoryIdentity(custom.parent, custom),
                    selected = true,
                    onClick = onOpenCategoryPicker,
                )
            }
            shown.forEach { category ->
                CategoryPill(
                    identity = categoryIdentity(category),
                    selected = selectedCustom == null && category == state.category,
                    onClick = { onCategoryChange(category) },
                )
            }
            DashedPill(
                label = "More",
                active = false,
                onClick = onOpenCategoryPicker,
            )
        }
    }
}

/** How many guesses the chip row offers before "More" takes over. */
private const val SUGGESTED_CHIP_COUNT = 3

/** Category colour and glyph on a pill — the same encoding the ledger rows use. */
@Composable
private fun CategoryPill(
    identity: CategoryIdentity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = identity.color
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
            identity.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = identity.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onSurface else diva.muted,
        )
    }
}

/**
 * The three optional enrichments, pinned above the save action.
 *
 * They sit at the bottom rather than in the form because none of them is part of entering
 * an expense — each is a detour the user chooses, and putting them between the amount and
 * the commit would make the common path read as if it had four more steps.
 */
@Composable
private fun ActionRow(
    state: QuickAddUiState,
    onOpenScanner: () -> Unit,
    onSplitSheetOpened: () -> Unit,
    onLocationChipToggled: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        ActionTile(
            label = "Scan Receipt",
            icon = Icons.Outlined.PhotoCamera,
            active = false,
            onClick = onOpenScanner,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            label = "Split",
            icon = Icons.Outlined.People,
            active = state.splitEnabled,
            // Income arrives whole; there is nobody to share it with.
            enabled = state.type == TransactionType.DEBIT,
            onClick = onSplitSheetOpened,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            label = if (state.location != null) "Location" else "Add Location",
            icon = Icons.Outlined.LocationOn,
            active = state.showLocation,
            onClick = onLocationChipToggled,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One square of the action row: glyph over label, tinted when its block is open. */
@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(BlockRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (active) diva.accent.copy(alpha = 0.12f) else diva.keyFill)
            .border(1.dp, if (active) diva.accent.copy(alpha = 0.4f) else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = when {
                !enabled -> diva.muted.copy(alpha = 0.4f)
                active -> diva.accent
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
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
private fun NoteRow(
    state: QuickAddUiState,
    onToggleDetails: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onMerchantSuggestionPicked: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    if (!state.showDetails) {
        // A ghost row, not a chip: nothing here is required, and giving it the weight of
        // a control would put a fourth thing between the amount and the save.
        Row(
            modifier = Modifier
                .clip(Pill)
                .clickable(onClick = onToggleDetails)
                .padding(vertical = Space.sm, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = diva.muted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = noteRowLabel(state),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = diva.muted,
            )
        }
        return
    }

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

        TagEditor(state.tags, onAddTag, onRemoveTag)
    }
}

/** Summarises what is already filled in, so collapsing the row does not hide it. */
private fun noteRowLabel(state: QuickAddUiState): String {
    val parts = buildList {
        if (state.merchantName.isNotBlank()) add(state.merchantName)
        if (state.note.isNotBlank()) add(state.note)
        if (state.tags.isNotEmpty()) add(state.tags.joinToString(" · "))
    }
    return if (parts.isEmpty()) "Add note or tags" else parts.joinToString(" · ")
}

/**
 * Tags as chips with a committing text field under them.
 *
 * Tags are orthogonal to the category — a transaction is filed in exactly one bucket
 * because reports have to count it once, and this is where the facts that cut across
 * buckets go instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(Pill)
                            .background(diva.accent.copy(alpha = 0.12f))
                            .clickable { onRemoveTag(tag) }
                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = diva.accent,
                        )
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove $tag",
                            tint = diva.accent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        DivaTextField(
            value = draft,
            onValueChange = { draft = it },
            label = "Add a tag",
            // Commits on the keyboard's Done rather than needing a button of its own —
            // the field is already the smallest thing on the screen.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onAddTag(draft)
                    draft = ""
                },
            ),
        )
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
private fun SplitSummary(state: QuickAddUiState, onOpen: () -> Unit) {
    if (!state.splitEnabled || state.type != TransactionType.DEBIT) return
    val split = state.split

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BlockRadius))
            .background(diva.keyFill)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.People,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Split ${state.splitOthers + 1} ways" +
                    (state.splitPaidBy?.let { " · ${it.name} paid" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state.splitOwnShare?.let {
                    "Your share ${formatCurrency(it, state.currency)}"
                } ?: "Shares don't add up yet",
                style = MaterialTheme.typography.bodySmall,
                color = if (split == null) MaterialTheme.colorScheme.error else diva.muted,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Edit the split",
            tint = diva.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * The calendar, as a sheet.
 *
 * Two chips for the days that are worth a shortcut, and a month grid for everything else.
 * Selection closes the sheet in the same gesture — a date does not need agreeing to twice.
 */
@Composable
private fun DateSheet(
    state: QuickAddUiState,
    onDismiss: () -> Unit,
    onDisplayedMonthChange: (LocalDate) -> Unit,
    onDateChange: (LocalDate) -> Unit,
) {
    val today = remember { todayDate() }
    val yesterday = remember(today) { LocalDate.fromEpochDays(today.toEpochDays() - 1) }

    DivaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(bottom = Space.xl)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            SheetTitle("Select Date", onDismiss)

            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                DateQuickChip("Today", state.date == today, Modifier.weight(1f)) {
                    onDateChange(today)
                }
                DateQuickChip("Yesterday", state.date == yesterday, Modifier.weight(1f)) {
                    onDateChange(yesterday)
                }
            }

            DivaCalendar(
                selected = state.date,
                displayedMonth = state.displayedMonth,
                onMonthChange = onDisplayedMonthChange,
                onSelect = onDateChange,
                maxDate = today,
            )

            Text(
                text = "Tap any past date to select it instantly",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DateQuickChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(Pill)
            .background(if (selected) diva.accent else diva.keyFill)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The contextual location ask, raised by the first save rather than on launch.
 *
 * This card is the reason the OS prompt cannot give for itself, which is why it comes
 * first — and why "Not now" is recorded rather than forgotten: an unsolicited question
 * that returns on the next entry is not a question, it is nagging.
 */
@Composable
private fun LocationConsentSheet(onAllow: () -> Unit, onDecline: () -> Unit) {
    DivaBottomSheet(onDismissRequest = onDecline) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(top = Space.md, bottom = Space.xl)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(diva.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = diva.accent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                "Use your location?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "See where you spent — Diva tags transactions with the place they " +
                    "happened, automatically. It stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = diva.muted,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                PillButton(
                    label = "Not now",
                    filled = false,
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    label = "Allow",
                    filled = true,
                    onClick = onAllow,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "You can turn this off anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A sheet's title row: heading left, dismiss right. */
@Composable
private fun SheetTitle(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            Icons.Outlined.Close,
            contentDescription = "Close",
            tint = diva.muted,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .padding(4.dp),
        )
    }
}

/**
 * The whole split, as a sheet: who paid, how it divides, and whether the parts add up.
 *
 * "Paid by" is a real choice rather than a label. Someone else paying inverts the debt —
 * the user owes them their share instead of being owed — and takes the charge off the
 * user's card entirely, because a bill they did not pay never touched it.
 */
@Composable
private fun SplitSheet(
    state: QuickAddUiState,
    onDismiss: () -> Unit,
    onSplitModeChange: (SplitMode) -> Unit,
    onSplitShareChange: (Int, Double) -> Unit,
    onSplitPayerChange: (SplitPerson?) -> Unit,
    onAddSplitPerson: (String) -> Unit,
    onRemoveSplitPerson: (String) -> Unit,
) {
    DivaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(bottom = Space.xl)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Split Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatCurrency(state.splitTotal ?: state.previewAmount ?: 0.0, state.currency),
                    style = NumericStyle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            FieldLabel("Paid by")
            PaidByRow(state, onSplitPayerChange, onAddSplitPerson, onRemoveSplitPerson)

            SegmentedControl(
                options = SplitMode.entries.map { it.label },
                selectedIndex = SplitMode.entries.indexOf(state.splitMode),
                modifier = Modifier.fillMaxWidth(),
                onSelect = { onSplitModeChange(SplitMode.entries[it]) },
            )

            ShareList(state, onSplitShareChange)

            AllocationCheck(state)

            DivaButton(
                text = "Add Split",
                onClick = onDismiss,
                enabled = state.split != null,
            )
        }
    }
}

/** Avatars for everyone on the bill; tapping one makes them the payer. */
@Composable
private fun PaidByRow(
    state: QuickAddUiState,
    onSplitPayerChange: (SplitPerson?) -> Unit,
    onAddSplitPerson: (String) -> Unit,
    onRemoveSplitPerson: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        PayerAvatar(
            name = "You",
            initials = "You",
            color = diva.accent,
            selected = state.splitPaidBy == null,
            onClick = { onSplitPayerChange(null) },
        )
        state.splitWith.forEach { person ->
            PayerAvatar(
                name = person.name,
                initials = initialsOf(person.name),
                color = personColor(person.name),
                selected = state.splitPaidBy?.name.equals(person.name, ignoreCase = true),
                onClick = { onSplitPayerChange(person) },
                onLongClick = { onRemoveSplitPerson(person.name) },
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .dashedCircle(diva.fgHair)
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
            Text("Add", style = MaterialTheme.typography.labelSmall, color = diva.muted)
        }
    }

    if (adding) {
        SplitPeopleRow(state, onAddSplitPerson, onRemoveSplitPerson)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PayerAvatar(
    name: String,
    initials: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(FieldRadius))
            .then(
                if (onLongClick == null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                }
            )
            .semantics { contentDescription = "$name paid" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Avatar(initials = initials, color = color, size = 44.dp)
            if (selected) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(2.dp, diva.accent, CircleShape)
                )
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else diva.muted,
            maxLines = 1,
        )
    }
}

/** Per-person shares — read-only when the split is even, editable in the manual modes. */
@Composable
private fun ShareList(state: QuickAddUiState, onSplitShareChange: (Int, Double) -> Unit) {
    val participants = state.allParticipants()
    val shares = state.split?.shares

    Column {
        participants.forEachIndexed { index, participant ->
            if (index > 0) Hairline()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    initials = if (index == 0) "You" else initialsOf(participant.name),
                    color = if (index == 0) diva.accent else personColor(participant.name),
                    size = 34.dp,
                )
                Text(
                    participant.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                when (state.splitMode) {
                    SplitMode.EQUALLY -> Text(
                        text = shares?.getOrNull(index)?.amountMinor?.toMajorUnits()
                            ?.let { formatCurrency(it, state.currency) } ?: "—",
                        style = NumericStyle.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SplitMode.BY_AMOUNT -> ShareField(
                        value = state.splitCustomAmounts.getOrNull(index) ?: 0.0,
                        suffix = state.currencyInfo.symbol,
                        label = "${participant.name}'s amount",
                        onChange = { onSplitShareChange(index, it) },
                    )
                    SplitMode.BY_PERCENT -> ShareField(
                        value = state.splitPercents.getOrNull(index) ?: 0.0,
                        suffix = "%",
                        label = "${participant.name}'s percent",
                        onChange = { onSplitShareChange(index, it) },
                    )
                }
            }
        }
    }
}

/**
 * One editable figure.
 *
 * The typed text is held locally and only parsed upward, so a half-typed "12." is not
 * rewritten under the user's cursor on every keystroke.
 */
@Composable
private fun ShareField(
    value: Double,
    suffix: String,
    label: String,
    onChange: (Double) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toFixed(2).trimEnd('0').trimEnd('.')) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { typed ->
                text = typed
                typed.toDoubleOrNull()?.let(onChange)
            },
            textStyle = NumericStyle.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(diva.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .width(74.dp)
                .clip(RoundedCornerShape(FieldRadius))
                .background(diva.keyFill)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .semantics { contentDescription = label },
        )
        Text(suffix, style = MaterialTheme.typography.bodyMedium, color = diva.muted)
    }
}

/**
 * "3 people · $57.80 of $57.80 allocated".
 *
 * Shown in every mode, including an even split where it can only ever be balanced: the
 * running total is the one line that says the bill is accounted for, and moving it in and
 * out as the mode changes would make its absence read as a problem.
 */
@Composable
private fun AllocationCheck(state: QuickAddUiState) {
    val allocation = state.allocation
    if (allocation == null) {
        Text(
            text = "Enter an amount to split.",
            style = MaterialTheme.typography.bodySmall,
            color = diva.muted,
        )
        return
    }
    val ok = allocation.isBalanced
    val figure: (Double) -> String = { value ->
        if (allocation.isPercent) "${value.toFixed(1).trimEnd('0').trimEnd('.')}%"
        else formatCurrency(value, state.currency)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldRadius))
            .background(
                if (ok) diva.positive.copy(alpha = 0.14f) else diva.negative.copy(alpha = 0.12f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (ok) Icons.Outlined.Check else Icons.Outlined.Info,
            contentDescription = null,
            tint = if (ok) diva.positive else diva.negative,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "${allocation.people} ${if (allocation.people == 1) "person" else "people"} · " +
                "${figure(allocation.allocated)} of ${figure(allocation.target)} allocated",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
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

            // The category and the date stay visible while the pad is up: totalling a
            // receipt is long enough that losing sight of what is being entered turns a
            // confirmed amount into a guess about which entry it belongs to.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter)
                    .padding(top = 4.dp, bottom = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val identity = categoryIdentity(
                    state.category,
                    state.customCategories.byId(state.customCategoryId),
                )
                ContextPill(identity.label, identity.color)
                ContextPill(state.dateLabel(), null)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = diva.accent,
                    modifier = Modifier
                        .clip(Pill)
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // The running sum sits *above* the result, in reading order: the expression is
            // what the user is building and the total is what it currently comes to.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter)
                    .padding(bottom = Space.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = state.expression.takeIf { it.any { c -> c in "+-*/()" } } ?: " ",
                    style = NumericStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = diva.muted,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.previewAmount
                            ?.let { formatCurrency(it, state.currency) }
                            ?: formatCurrency(0.0, state.currency),
                        style = NumericStyle.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                        ),
                        color = if (state.previewAmount == null) {
                            diva.muted.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                    )
                    Text(
                        text = " ${state.currency}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = diva.muted,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            CalculatorKeypad(
                onDigit = onDigit,
                onOperator = onOperator,
                onBackspace = onBackspace,
                modifier = Modifier.padding(horizontal = 13.dp),
                extended = true,
                onGroup = onGroup,
                onClear = onClear,
                onEquals = onEquals,
            )
        }
    }
}

/** A read-only reminder of what the amount is being entered against. */
@Composable
private fun ContextPill(label: String, tint: Color?) {
    Box(
        modifier = Modifier
            .clip(Pill)
            .background((tint ?: diva.muted).copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tint ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
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

/**
 * A dashed ring, for the seat on the bill nobody is in yet. Dashes say "not filled in"
 * where a solid rule would read as one more person already on it.
 */
private fun Modifier.dashedCircle(color: Color, width: Dp = 1.5.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawCircle(
        color = color,
        radius = (size.minDimension - stroke) / 2f,
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2.5f, stroke * 2.5f)),
        ),
    )
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
private fun AddExpenseSplitSheetPreview() {
    DivaTheme {
        AddExpenseContent(
            state = QuickAddUiState(
                expression = "57.80",
                category = SpendingCategory.DINING,
                splitEnabled = true,
                splitSheetOpen = true,
                splitWith = listOf(SplitPerson(null, "Sam"), SplitPerson(null, "Priya")),
                splitWithCount = 2,
            ),
        )
    }
}

@Preview
@Composable
private fun AddExpenseDatePreview() {
    DivaTheme {
        AddExpenseContent(state = QuickAddUiState(expression = "24", dateSheetOpen = true))
    }
}

@Preview
@Composable
private fun AddExpenseLocationConsentPreview() {
    DivaTheme {
        AddExpenseContent(state = QuickAddUiState(expression = "24", locationSheetOpen = true))
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
