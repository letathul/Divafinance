package com.divafinance.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.divafinance.core.model.enums.SpendingCategory

/**
 * The swatches the add-person form offers.
 *
 * A fixed six rather than the category ramp: those twelve hues are this app's data encoding
 * for *what* was spent, and letting a person wear one would make the same colour mean two
 * things on the same screen. Six is also as many as fit a row at a tappable size.
 */
val PersonPalette: List<Color> = listOf(
    Color(0xFFD1594B), // clay
    Color(0xFF4A7FD1), // blue
    Color(0xFFB25FA8), // orchid
    Color(0xFFD1893F), // amber
    Color(0xFF56A36B), // fern
    Color(0xFF3FA79A), // teal
)

/**
 * The colour to draw someone's avatar in.
 *
 * [colorHex] is what they picked; absent — every person written before the column existed —
 * falls back to the stable hash of the name, so nobody's avatar changes colour on upgrade.
 */
@Composable
fun personColor(name: String, colorHex: String? = null): Color =
    colorHex?.let { parseHexColor(it) } ?: hashedPersonColor(name)

/** A stable colour per name, so the same person keeps the same avatar between entries. */
@Composable
private fun hashedPersonColor(name: String): Color {
    val ramp = SpendingCategory.entries
    val index = (name.lowercase().sumOf { it.code } % ramp.size)
    return ramp[index].color
}

/** `#RRGGBB` for a swatch, which is what the `Person` row stores. */
fun Color.toHex(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    fun pair(v: Int) = v.toString(16).padStart(2, '0')
    return "#${pair(r)}${pair(g)}${pair(b)}"
}
