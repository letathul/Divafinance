package com.divafinance.feature.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.component.CategoryIconKeys
import com.divafinance.core.ui.component.CategoryIdentity
import com.divafinance.core.ui.component.CategoryPickerItem
import com.divafinance.core.ui.component.CategoryTile
import com.divafinance.core.ui.component.DivaButton
import com.divafinance.core.ui.component.DivaTextField
import com.divafinance.core.ui.component.categoryIconForKey
import com.divafinance.core.ui.component.categoryIdentity
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva

/**
 * The full category list, reached when the three guesses on the add screen don't match.
 *
 * A screen rather than a sheet: it carries a search field, a recents row and a grid that
 * grows with every category the user invents, and a sheet that tall is a screen that has
 * to be dragged.
 *
 * It writes straight back into the shared [QuickAddViewModel] — the same instance the add
 * screen is using — so picking a category needs no navigation result plumbing.
 */
@Composable
fun CategoryPickerScreen(
    onDone: () -> Unit,
    viewModel: QuickAddViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    CategoryPickerContent(
        state = state,
        modifier = modifier,
        onDismiss = onDone,
        onCategoryChange = {
            viewModel.onCategoryChange(it)
            onDone()
        },
        onCustomCategoryChange = {
            viewModel.onCustomCategoryChange(it)
            onDone()
        },
        onCreateCustomCategory = { name, iconKey, colorHex, parent ->
            viewModel.onCreateCustomCategory(name, iconKey, colorHex, parent)
            onDone()
        },
    )
}

/** Stateless body, so tests and previews can drive it without a ViewModel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryPickerContent(
    state: QuickAddUiState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onCategoryChange: (SpendingCategory) -> Unit = {},
    onCustomCategoryChange: (CustomCategory) -> Unit = {},
    onCreateCustomCategory: (String, String, String, SpendingCategory) -> Unit = { _, _, _, _ -> },
) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(diva.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.pad)
                .padding(top = Space.md, bottom = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = diva.muted,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(6.dp),
                )
            }

            DivaTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search categories",
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = diva.muted)
                },
            )

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                // Recents are the prediction the add screen already trusts, shown here as
                // the same answer in a longer list rather than a second ranking.
                if (query.isBlank() && state.suggestedCategories.isNotEmpty()) {
                    PickerSectionLabel("Recent")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        state.suggestedCategories.forEach { category ->
                            RecentChip(category.displayName) { onCategoryChange(category) }
                        }
                    }
                }

                PickerSectionLabel("All categories")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    state.customCategories
                        .filter { it.name.matches(query) }
                        .forEach { custom ->
                            CategoryPickerItem(
                                identity = categoryIdentity(custom.parent, custom),
                                selected = custom.id == state.customCategoryId,
                                onClick = { onCustomCategoryChange(custom) },
                            )
                        }

                    SpendingCategory.entries
                        .filter { it.displayName.matches(query) }
                        .forEach { category ->
                            CategoryPickerItem(
                                category = category,
                                selected = state.customCategoryId == null &&
                                    category == state.category,
                                onClick = { onCategoryChange(category) },
                            )
                        }

                    if (query.isBlank()) {
                        AddCustomTile { creating = true }
                    }
                }
            }
        }

        if (creating) {
            CustomCategoryCreator(
                onDismiss = { creating = false },
                onCreate = onCreateCustomCategory,
            )
        }
    }
}

/** Case-insensitive contains, with a blank query matching everything. */
private fun String.matches(query: String): Boolean =
    query.isBlank() || contains(query.trim(), ignoreCase = true)

@Composable
private fun PickerSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = diva.muted,
    )
}

@Composable
private fun RecentChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(com.divafinance.core.ui.theme.Pill)
            .background(diva.keyFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The dashed tile that opens the creator. Same footprint as a category so the grid holds. */
@Composable
private fun AddCustomTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(66.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(diva.accent.copy(alpha = 0.12f))
                .border(1.dp, diva.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = diva.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "Add custom",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = diva.accent,
            maxLines = 1,
        )
    }
}

/**
 * Name, glyph, hue, and the built-in it behaves as.
 *
 * That last field is the one that looks optional and is not. `SpendingCategory` is the
 * axis every engine works on — reward rules are keyed by it, thresholds are keyed by it —
 * so a category with no parent would earn no rewards, predict from no history and fall
 * out of every report. Picking one is what makes "Ramen" behave like dining everywhere
 * except on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomCategoryCreator(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, SpendingCategory) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf(CategoryIconKeys.first()) }
    var parent by remember { mutableStateOf(SpendingCategory.OTHER) }

    com.divafinance.core.ui.adaptive.DivaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.pad)
                .padding(bottom = Space.xl)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Text(
                "New category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            DivaTextField(value = name, onValueChange = { name = it }, label = "Name")

            PickerSectionLabel("Icon")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                CategoryIconKeys.forEach { key ->
                    val selected = key == iconKey
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) diva.accent.copy(alpha = 0.16f) else diva.keyFill
                            )
                            .border(
                                1.dp,
                                if (selected) diva.accent else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { iconKey = key }
                            .semantics { contentDescription = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        categoryIconForKey(key)?.let {
                            Icon(
                                it,
                                contentDescription = null,
                                tint = if (selected) diva.accent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            PickerSectionLabel("Behaves as")
            Text(
                "Rewards, predictions and reports treat this category as the one you pick here.",
                style = MaterialTheme.typography.bodySmall,
                color = diva.muted,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                SpendingCategory.entries.forEach { candidate ->
                    val selected = candidate == parent
                    Box(
                        modifier = Modifier
                            .clip(com.divafinance.core.ui.theme.Pill)
                            .background(
                                if (selected) diva.accent.copy(alpha = 0.14f) else diva.keyFill
                            )
                            .border(
                                1.dp,
                                if (selected) diva.accent else Color.Transparent,
                                com.divafinance.core.ui.theme.Pill,
                            )
                            .clickable { parent = candidate }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            candidate.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) diva.accent else diva.muted,
                        )
                    }
                }
            }

            // The colour follows the parent's ramp entry rather than being a fourth
            // decision: the ramp is how this app encodes category, and letting a custom
            // category pick freely would let two categories collide on one hue.
            val parentIdentity = categoryIdentity(parent)
            val preview = CategoryIdentity(
                label = name.ifBlank { "New category" },
                icon = categoryIconForKey(iconKey) ?: parentIdentity.icon,
                color = parentIdentity.color,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                CategoryTile(preview, size = 44.dp)
                Text(
                    preview.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(Space.xs))

            DivaButton(
                text = "Add category",
                onClick = { onCreate(name, iconKey, parentIdentity.color.toHex(), parent) },
                enabled = name.isNotBlank(),
            )
        }
    }
}

/** `#RRGGBB`, matching what `parseHexColor` reads back. */
private fun Color.toHex(): String {
    fun channel(value: Float): String =
        ((value * 255f).toInt().coerceIn(0, 255)).toString(16).padStart(2, '0')
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}
