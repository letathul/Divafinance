package com.divafinance.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.divafinance.core.model.CustomCategory
import com.divafinance.core.model.enums.SpendingCategory

/**
 * How a category should be *shown* — resolved once so no screen has to know whether it is
 * looking at a built-in or one the user invented.
 *
 * Deliberately not a `SpendingCategory`: the enum stays the reporting axis, and this is
 * only the label/glyph/hue over it.
 */
data class CategoryIdentity(
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

/**
 * The icons a custom category may choose from.
 *
 * Keys, not indices — the list can be reordered or grown without re-pointing every stored
 * `icon_key`. There is no asset pipeline in this app, so these are all
 * `Icons.Outlined.*`: stroke glyphs, never emoji, matching every other category glyph.
 */
val CategoryIconKeys: List<String> = listOf(
    "restaurant", "cafe", "bar", "pizza", "cart", "bag",
    "car", "bus", "train", "flight", "fuel", "home",
    "bolt", "wifi", "phone", "gym", "hospital", "school",
    "pets", "movie", "music", "games", "gift", "savings",
    "work", "tools", "clothes", "spa", "party",
)

/** Null for an unknown key, so callers can fall back to the parent category's glyph. */
fun categoryIconForKey(key: String): ImageVector? = when (key) {
    "restaurant" -> Icons.Outlined.Restaurant
    "cafe" -> Icons.Outlined.LocalCafe
    "bar" -> Icons.Outlined.LocalBar
    "pizza" -> Icons.Outlined.LocalPizza
    "cart" -> Icons.Outlined.ShoppingCart
    "bag" -> Icons.Outlined.ShoppingBag
    "car" -> Icons.Outlined.DirectionsCar
    "bus" -> Icons.Outlined.DirectionsBus
    "train" -> Icons.Outlined.Train
    "flight" -> Icons.Outlined.Flight
    "fuel" -> Icons.Outlined.LocalGasStation
    "home" -> Icons.Outlined.Home
    "bolt" -> Icons.Outlined.Bolt
    "wifi" -> Icons.Outlined.Wifi
    "phone" -> Icons.Outlined.Phone
    "gym" -> Icons.Outlined.FitnessCenter
    "hospital" -> Icons.Outlined.LocalHospital
    "school" -> Icons.Outlined.School
    "pets" -> Icons.Outlined.Pets
    "movie" -> Icons.Outlined.Movie
    "music" -> Icons.Outlined.MusicNote
    "games" -> Icons.Outlined.SportsEsports
    "gift" -> Icons.Outlined.CardGiftcard
    "savings" -> Icons.Outlined.Savings
    "work" -> Icons.Outlined.Work
    "tools" -> Icons.Outlined.Build
    "clothes" -> Icons.Outlined.Checkroom
    "spa" -> Icons.Outlined.Spa
    "party" -> Icons.Outlined.Celebration
    else -> null
}

/**
 * `#RRGGBB` / `#AARRGGBB` to a [Color], or null when the string is not one.
 *
 * Colours are stored as text the user's own data can carry, so a malformed value has to
 * degrade rather than throw — a bad hex should cost the custom hue, not the whole screen.
 */
fun parseHexColor(hex: String): Color? {
    val body = hex.removePrefix("#")
    if (body.length != 6 && body.length != 8) return null
    val value = body.toLongOrNull(16) ?: return null
    return Color(if (body.length == 6) value or 0xFF000000L else value)
}

/**
 * The identity to render with. [custom] wins on every field it can supply, falling back to
 * [category] per-field rather than wholesale: a custom category with an unrecognised icon
 * key still keeps its own name and colour.
 */
@Composable
fun categoryIdentity(
    category: SpendingCategory,
    custom: CustomCategory? = null,
): CategoryIdentity = CategoryIdentity(
    label = custom?.name?.takeIf { it.isNotBlank() } ?: category.displayName,
    icon = custom?.let { categoryIconForKey(it.iconKey) } ?: category.icon,
    color = custom?.let { parseHexColor(it.colorHex) } ?: category.color,
)

/** Resolves the custom category a transaction was filed under, by id. */
fun List<CustomCategory>.byId(id: String?): CustomCategory? =
    id?.let { wanted -> firstOrNull { it.id == wanted } }
