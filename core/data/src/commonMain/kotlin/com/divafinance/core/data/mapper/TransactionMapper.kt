package com.divafinance.core.data.mapper

import com.divafinance.core.database.DivaTransaction
import com.divafinance.core.database.SelectWithLocation
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * The single authority for turning a stored transaction row into a [Transaction].
 *
 * SQLDelight generates a distinct row type per query shape — `selectWithLocation` narrows
 * latitude/longitude to non-null, so it does not share [DivaTransaction]. Both row types
 * funnel through [toDomain] so the mapping body exists exactly once.
 */
object TransactionMapper {

    /**
     * Categories are read back by name, so a renamed or removed enum constant would make
     * every historical row unreadable. Unknown names degrade to [SpendingCategory.OTHER]
     * rather than throwing the way `valueOf` does.
     */
    fun categoryOf(name: String): SpendingCategory =
        SpendingCategory.entries.firstOrNull { it.name == name } ?: SpendingCategory.OTHER

    /**
     * `type` drives whether a row counts as spending. An unrecognised value means the row is
     * corrupt; treating it as a debit keeps it visible in spending totals instead of silently
     * inflating income.
     */
    fun typeOf(name: String): TransactionType =
        TransactionType.entries.firstOrNull { it.name == name } ?: TransactionType.DEBIT

    fun toDomain(
        id: String,
        accountId: String,
        cardId: String?,
        amount: Double,
        currency: String,
        category: String,
        subcategory: String?,
        merchantName: String?,
        note: String?,
        date: String,
        type: String,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        receiptId: String?,
        isRecurring: Boolean,
        createdAt: String,
        othersShare: Double = 0.0,
    ): Transaction = Transaction(
        id = id,
        accountId = accountId,
        cardId = cardId,
        amount = amount,
        currency = currency,
        category = categoryOf(category),
        subcategory = subcategory,
        merchantName = merchantName,
        note = note,
        date = LocalDate.parse(date),
        type = typeOf(type),
        location = if (latitude != null && longitude != null) {
            LocationTag(latitude, longitude, locationName)
        } else null,
        receiptId = receiptId,
        isRecurring = isRecurring,
        createdAt = Instant.parse(createdAt),
        othersShare = othersShare,
    )

    fun toMap(transaction: Transaction): Map<String, Any?> = mapOf(
        "id" to transaction.id,
        "account_id" to transaction.accountId,
        "card_id" to transaction.cardId,
        "amount" to transaction.amount,
        "currency" to transaction.currency,
        "category" to transaction.category.name,
        "subcategory" to transaction.subcategory,
        "merchant_name" to transaction.merchantName,
        "note" to transaction.note,
        "date" to transaction.date.toString(),
        "type" to transaction.type.name,
        "latitude" to transaction.location?.latitude,
        "longitude" to transaction.location?.longitude,
        "location_name" to transaction.location?.name,
        "receipt_id" to transaction.receiptId,
        "is_recurring" to transaction.isRecurring,
        "created_at" to transaction.createdAt.toString(),
        "others_share" to transaction.othersShare,
    )
}

fun DivaTransaction.toDomain(): Transaction = TransactionMapper.toDomain(
    id = id,
    accountId = account_id,
    cardId = card_id,
    amount = amount,
    currency = currency,
    category = category,
    subcategory = subcategory,
    merchantName = merchant_name,
    note = note,
    date = date,
    type = type,
    latitude = latitude,
    longitude = longitude,
    locationName = location_name,
    receiptId = receipt_id,
    isRecurring = is_recurring == 1L,
    createdAt = created_at,
    othersShare = others_share,
)

fun SelectWithLocation.toDomain(): Transaction = TransactionMapper.toDomain(
    id = id,
    accountId = account_id,
    cardId = card_id,
    amount = amount,
    currency = currency,
    category = category,
    subcategory = subcategory,
    merchantName = merchant_name,
    note = note,
    date = date,
    type = type,
    latitude = latitude,
    longitude = longitude,
    locationName = location_name,
    receiptId = receipt_id,
    isRecurring = is_recurring == 1L,
    createdAt = created_at,
    othersShare = others_share,
)
