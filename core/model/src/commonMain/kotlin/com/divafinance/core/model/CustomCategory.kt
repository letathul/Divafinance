package com.divafinance.core.model

import com.divafinance.core.model.enums.SpendingCategory
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * A category the user made up, shown in place of one of the twelve built-ins.
 *
 * This is a **display identity, not a new dimension**. [SpendingCategory] stays the axis
 * every engine works on — reward rules are keyed by it, thresholds are keyed by it, and
 * [com.divafinance.core.model.Transaction.category] is still always one of the twelve. A
 * custom category only changes the name, icon and colour a row is *shown* with.
 *
 * [parent] is what makes that work: "Ramen" carries `DINING`, so it earns the same
 * rewards, predicts the same way and lands in the same report bucket as dining always
 * did. Without it, a user-invented category would have no reward rate, no threshold and
 * no history to predict from — it would be a hole in every engine rather than a label.
 */
@Serializable
data class CustomCategory(
    val id: String,
    val name: String,
    /**
     * A key into `core:ui`'s icon registry, not a drawable name. The app has no asset
     * pipeline, so icons are `Icons.Outlined.*` looked up by key; an unknown key falls
     * back to the [parent]'s icon rather than rendering nothing.
     */
    val iconKey: String,
    /** `#RRGGBB`. Free-form rather than a ramp index so the ramp can change independently. */
    val colorHex: String,
    /** The built-in this behaves as everywhere except on screen. */
    val parent: SpendingCategory,
    val createdAt: Instant,
)
