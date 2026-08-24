package com.divafinance.feature.scanner.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image as FoundationImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import com.divafinance.core.common.toFixed
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.ui.component.CategoryPickerItem
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaOutlinedButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.Hairline
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.SectionHeader
import com.divafinance.core.ui.component.SegmentedControl
import com.divafinance.core.ui.theme.NumericStyle
import com.divafinance.core.ui.util.formatCurrency
import com.divafinance.core.ui.theme.diva
import com.divafinance.feature.scanner.capture.rememberReceiptThumbnail
import org.koin.compose.viewmodel.koinViewModel

/** Ordered to match `TransactionType.entries`, which is what the control indexes into. */
private val TYPE_OPTIONS = listOf("Purchase", "Refund")

/**
 * The step between a scan and a transaction: everything OCR guessed, editable, plus the
 * category prediction. Saving here is the only thing that links a receipt to a transaction.
 */
@Composable
fun ReceiptReviewScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: ReceiptReviewViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved != null) onSaved()
    }

    DivaScaffold(
        title = "Review receipt",
        onBack = onBack,
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@DivaScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Thumbnail beside what was read off it, so the scan and its interpretation
            // are the same glance rather than two.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(diva.keyFill)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReceiptPreview(uiState.receipt?.imagePath)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        uiState.merchantName.ifBlank { "Untitled receipt" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Meta(
                        "Scanned" + (uiState.dateText.takeIf { it.isNotBlank() }
                            ?.let { " · $it" } ?: "")
                    )
                }
            }

            if (uiState.pagePaths.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ReceiptExtraPages(uiState.pagePaths)
            }
            Spacer(Modifier.height(16.dp))

            DivaTextField(
                value = uiState.merchantName,
                onValueChange = viewModel::onMerchantChanged,
                label = "Merchant",
            )
            Spacer(Modifier.height(8.dp))
            DivaTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChanged,
                label = "Amount",
                isError = uiState.amountText.isNotBlank() && uiState.amount == null,
                supportingText = if (uiState.amountText.isNotBlank() && uiState.amount == null) {
                    "Enter an amount greater than zero"
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DivaTextField(
                    value = uiState.dateText,
                    onValueChange = viewModel::onDateTextChanged,
                    label = "Date",
                    isError = uiState.hasDateError,
                    supportingText = if (uiState.hasDateError) "Use YYYY-MM-DD" else null,
                    modifier = Modifier.weight(2f),
                )
                DivaTextField(
                    value = uiState.currency,
                    onValueChange = viewModel::onCurrencyChanged,
                    label = "Currency",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            DivaTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = "Note (optional)",
            )

            Spacer(Modifier.height(16.dp))
            // A receipt is usually a purchase, but a refund is printed on one too.
            SegmentedControl(
                options = TYPE_OPTIONS,
                selectedIndex = uiState.type.ordinal,
                onSelect = { viewModel.onTypeSelected(TransactionType.entries[it]) },
            )

            if (uiState.items.isNotEmpty() || uiState.taxAmount != null) {
                Spacer(Modifier.height(20.dp))
                ItemisedSection(
                    items = uiState.items,
                    itemsTotal = uiState.itemsTotal,
                    taxAmount = uiState.taxAmount,
                    tipAmount = uiState.tipAmount,
                    disagrees = uiState.itemsDisagreeWithTotal,
                    currency = uiState.currency,
                    onDescriptionChange = viewModel::onItemDescriptionChanged,
                    onQuantityChange = viewModel::onItemQuantityChanged,
                    onPriceChange = viewModel::onItemPriceChanged,
                    onRemove = viewModel::onItemRemoved,
                    onAdd = viewModel::onItemAdded,
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Category")
            Spacer(Modifier.height(8.dp))
            CategoryRow(
                categories = uiState.suggestedCategories,
                selected = uiState.category,
                onSelect = viewModel::onCategorySelected,
            )

            if (uiState.cards.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionHeader("Paid with")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.cards.forEach { card ->
                        FilterChip(
                            selected = uiState.selectedCardId == card.id,
                            // Tapping the selected card clears it — a receipt paid in cash
                            // has no card, and there is no other way back to that.
                            onClick = {
                                viewModel.onCardSelected(
                                    card.id.takeIf { it != uiState.selectedCardId },
                                )
                            },
                            label = { Text(card.name) },
                        )
                    }
                }
            }

            uiState.betterCard?.let { recommendation ->
                Spacer(Modifier.height(12.dp))
                BetterCardNudge(
                    cardName = recommendation.card.name,
                    rewardValue = recommendation.estimatedRewardValue,
                    currency = uiState.currency,
                    onUse = { viewModel.onCardSelected(recommendation.card.id) },
                )
            }

            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            // The figure this whole screen exists to get right, stated once at the end
            // rather than left implicit in the amount field halfway up.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "Grand total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "What this transaction will be saved for",
                        style = MaterialTheme.typography.bodySmall,
                        color = diva.muted,
                    )
                }
                Text(
                    text = uiState.amount
                        ?.let { formatCurrency(it, uiState.currency) }
                        ?: "—",
                    style = NumericStyle.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = if (uiState.amount == null) {
                        diva.muted.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }

            Spacer(Modifier.height(14.dp))
            if (uiState.isSaving) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                DivaButton(
                    text = "Save transaction",
                    onClick = viewModel::save,
                    enabled = uiState.canSave,
                )
            }

            uiState.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<SpendingCategory>,
    selected: SpendingCategory,
    onSelect: (SpendingCategory) -> Unit,
) {
    // The prediction is always padded to a full row, so this never renders short.
    val shown = if (selected in categories) categories else listOf(selected) + categories
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        shown.forEach { category ->
            CategoryPickerItem(
                category = category,
                selected = category == selected,
                onClick = { onSelect(category) },
            )
        }
    }
}

/**
 * What the receipt printed besides the total. Editable, because the point of showing it is
 * that the user can fix a misread line rather than only look at one.
 */
@Composable
private fun ItemisedSection(
    items: List<LineItemDraft>,
    itemsTotal: Double?,
    taxAmount: Double?,
    tipAmount: Double?,
    disagrees: Boolean,
    currency: String,
    onDescriptionChange: (String, String) -> Unit,
    onQuantityChange: (String, String) -> Unit,
    onPriceChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    // A printed receipt is a table, so this is one: reading the scan against the screen is
    // the whole task here, and a stack of labelled fields makes that a line-by-line hunt.
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColumnLabel("Item", Modifier.weight(2.6f))
        ColumnLabel("Qty", Modifier.weight(1f), TextAlign.Center)
        ColumnLabel("Price", Modifier.weight(1.3f), TextAlign.Center)
        ColumnLabel("Total", Modifier.weight(1.3f), TextAlign.End)
    }
    Hairline()

    Column {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CellField(
                    value = item.description,
                    onValueChange = { onDescriptionChange(item.id, it) },
                    label = "Item ${items.indexOf(item) + 1} description",
                    modifier = Modifier.weight(2.6f),
                )
                CellField(
                    value = item.quantityText,
                    onValueChange = { onQuantityChange(item.id, it) },
                    label = "${item.description.ifBlank { "Item" }} quantity",
                    placeholder = "1",
                    numeric = true,
                    align = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                CellField(
                    value = item.unitPriceText,
                    onValueChange = { onPriceChange(item.id, it) },
                    label = "${item.description.ifBlank { "Item" }} unit price",
                    numeric = true,
                    align = TextAlign.Center,
                    modifier = Modifier.weight(1.3f),
                )
                // Computed, not typed: a line total that disagrees with quantity times
                // price is a receipt nobody can reconcile.
                Text(
                    text = item.price?.toFixed(2) ?: "—",
                    style = NumericStyle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.weight(1.3f),
                )
                IconButton(
                    onClick = { onRemove(item.id) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove ${item.description.ifBlank { "item" }}",
                        tint = diva.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Hairline()
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DivaOutlinedButton(text = "Add item", onClick = onAdd)
        Column(horizontalAlignment = Alignment.End) {
            itemsTotal?.let { Meta("Items ${formatCurrency(it, currency)}") }
            taxAmount?.let { Meta("Tax ${formatCurrency(it, currency)}") }
            tipAmount?.let { Meta("Tip ${formatCurrency(it, currency)}") }
        }
    }

    if (disagrees) {
        Spacer(Modifier.height(8.dp))
        // Deliberately not blocking the save: a discount or deposit line this parser doesn't
        // recognise makes the sums disagree on a receipt that is perfectly fine.
        Text(
            "The items, tax and tip don't add up to the total — worth a second look.",
            style = MaterialTheme.typography.bodySmall,
            color = diva.muted,
        )
    }
}

@Composable
private fun ColumnLabel(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = diva.muted,
        textAlign = align,
        modifier = modifier,
    )
}

/**
 * One cell of the table: a filled field with no label chrome, because the column heading
 * above it already says what it is and four labelled boxes per row would be unreadable.
 */
@Composable
private fun CellField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    numeric: Boolean = false,
    align: TextAlign = TextAlign.Start,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(diva.keyFill)
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                placeholder,
                style = if (numeric) NumericStyle else MaterialTheme.typography.bodyMedium,
                color = diva.muted.copy(alpha = 0.7f),
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = (if (numeric) NumericStyle else MaterialTheme.typography.bodyMedium)
                .copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = align,
                ),
            cursorBrush = SolidColor(diva.accent),
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Decimal)
            } else {
                KeyboardOptions.Default
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
        )
    }
}

/**
 * Surfaced only when a different card would have earned more, and offered as a one-tap
 * correction: the recommendation is worthless if acting on it means going back to the chips.
 */
@Composable
private fun BetterCardNudge(
    cardName: String,
    rewardValue: Double,
    currency: String,
    onUse: () -> Unit,
) {
    DivaCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$cardName earns more here", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Meta("About ${rewardValue.toFixed(2)} $currency back")
            }
            DivaOutlinedButton(text = "Use it", onClick = onUse)
        }
    }
}

/**
 * The photo, where the platform can decode one, plus a strip for the extra pages of a
 * multi-page scan. The placeholder is not a failure state — most targets have no common file
 * decoder — so it reads as "photo attached" rather than an error.
 */
@Composable
private fun ReceiptExtraPages(pagePaths: List<String>) {
    Meta("${pagePaths.size + 1} pages")
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pagePaths.forEach { page ->
            val thumbnail = rememberReceiptThumbnail(page, maxDimension = 128)
            DivaCard {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbnail != null) {
                        FoundationImage(
                            bitmap = thumbnail,
                            contentDescription = "Additional receipt page",
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = diva.muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptPreview(imagePath: String?) {
    val thumbnail = rememberReceiptThumbnail(imagePath)
    DivaCard {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                FoundationImage(
                    bitmap = thumbnail,
                    contentDescription = "Scanned receipt",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = diva.muted,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Meta(if (imagePath != null) "Photo attached" else "No photo")
                }
            }
        }
    }
}
