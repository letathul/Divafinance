package com.divafinance.feature.scanner.review

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image as FoundationImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.CategoryPickerItem
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.core.ui.component.Meta
import com.divafinance.core.ui.component.SectionHeader
import com.divafinance.core.ui.theme.diva
import com.divafinance.feature.scanner.capture.rememberReceiptThumbnail
import org.koin.compose.viewmodel.koinViewModel

/**
 * The step between a scan and a transaction: everything OCR guessed, editable, plus the
 * category prediction. Saving here is the only thing that links a receipt to a transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ReceiptPreview(uiState.receipt?.imagePath)
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
            uiState.date?.let { Meta("Dated $it") }

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

            Spacer(Modifier.height(24.dp))
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
 * The photo, where the platform can decode one. The placeholder is not a failure state — most
 * targets have no common file decoder — so it reads as "photo attached" rather than an error.
 */
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
