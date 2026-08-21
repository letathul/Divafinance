package com.divafinance.core.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Someone money is owed to or from. Local to this device — not a contact, not an account,
 * and never shared anywhere.
 */
@Serializable
data class Person(
    val id: String,
    val name: String,
    /** Free text: "flatmate", a phone number, whatever helps tell two Sams apart. */
    val note: String? = null,
    /**
     * Archived rather than deleted. `PRAGMA foreign_keys` is off across this app, so a
     * hard delete would silently orphan their ledger rows instead of being refused.
     */
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
