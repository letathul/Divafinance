package com.divafinance.feature.demo

/**
 * Lifecycle of the one-time demo offer.
 *
 * The offer is deliberately one-way: once it leaves [UNDECIDED] it can only ever end at
 * [RETIRED], and [RETIRED] is terminal. Nothing in the app moves the status backwards, so
 * a user who has declined or removed the demo is never asked again.
 *
 * ```
 * UNDECIDED ──accept──> ACTIVE ──remove──> RETIRED
 *     └──────decline───────────────────────────┘
 * ```
 */
enum class DemoStatus {
    /** No decision recorded yet — the only state in which the offer is shown. */
    UNDECIDED,

    /** Demo data is seeded. Settings offers a one-way "remove" action. */
    ACTIVE,

    /** Declined or removed. The offer is never shown again anywhere. */
    RETIRED,

    ;

    companion object {
        /** Unknown/garbage persisted values fall back to [UNDECIDED]. */
        fun fromStorage(raw: String?): DemoStatus =
            entries.firstOrNull { it.name == raw } ?: UNDECIDED
    }
}
