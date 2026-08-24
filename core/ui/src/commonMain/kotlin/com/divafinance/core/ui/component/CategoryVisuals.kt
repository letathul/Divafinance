package com.divafinance.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.ui.theme.diva

/** The category's hue from the encoding ramp. Theme-aware: light mode has its own set. */
val SpendingCategory.color: Color
    @Composable get() = diva.categoryColor(this)

val SpendingCategory.icon: ImageVector
    get() = when (this) {
        SpendingCategory.DINING -> Icons.Outlined.LocalCafe
        SpendingCategory.GAS -> Icons.Outlined.LocalGasStation
        SpendingCategory.GROCERIES -> Icons.Outlined.ShoppingCart
        SpendingCategory.TRANSPORTATION -> Icons.Outlined.DirectionsCar
        SpendingCategory.UTILITIES -> Icons.AutoMirrored.Outlined.ReceiptLong
        SpendingCategory.TRAVEL -> Icons.Outlined.Flight
        SpendingCategory.SUBSCRIPTIONS -> Icons.Outlined.Replay
        SpendingCategory.ENTERTAINMENT -> Icons.Outlined.ConfirmationNumber
        SpendingCategory.SHOPPING -> Icons.Outlined.ShoppingBag
        SpendingCategory.HEALTHCARE -> Icons.Outlined.LocalHospital
        SpendingCategory.EDUCATION -> Icons.AutoMirrored.Outlined.MenuBook
        SpendingCategory.OTHER -> Icons.Outlined.AutoAwesome
    }

/**
 * The rounded-square glyph tile that fronts every ledger row: category colour at 16% over
 * the surface with a 22% ring. Tinted, never saturated — the palette encodes data, it
 * does not decorate.
 */
@Composable
fun CategoryTile(
    category: SpendingCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    selected: Boolean = false,
) = CategoryTile(categoryIdentity(category), modifier, size, selected)

/**
 * The same tile over an already-resolved [CategoryIdentity], so a user's own category
 * renders through exactly one code path with the built-in twelve.
 */
@Composable
fun CategoryTile(
    identity: CategoryIdentity,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    selected: Boolean = false,
) {
    val c = identity.color
    val shape = RoundedCornerShape(if (size >= 44.dp) 14.dp else 12.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(
                if (selected) c
                else c.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surface)
            )
            .border(1.dp, if (selected) Color.Transparent else c.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            identity.icon,
            contentDescription = identity.label,
            tint = if (selected) MaterialTheme.colorScheme.background else c,
            modifier = Modifier.size(size * 0.47f),
        )
    }
}

/** The legend dot used in breakdowns and filter chips. */
@Composable
fun CategoryDot(color: Color, modifier: Modifier = Modifier, size: Dp = 9.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Vertical tile + label, as used by the amount screen's category picker. */
@Composable
fun CategoryPickerItem(
    category: SpendingCategory,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = CategoryPickerItem(categoryIdentity(category), selected, modifier, onClick)

/** As above, over an already-resolved identity. */
@Composable
fun CategoryPickerItem(
    identity: CategoryIdentity,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .width(66.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryTile(identity, size = 52.dp, selected = selected)
        Text(
            identity.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else diva.muted,
            maxLines = 1,
        )
    }
}
