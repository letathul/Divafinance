package com.divafinance.core.data.mapper

import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

object TransactionMapper {

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
    ): Transaction = Transaction(
        id = id,
        accountId = accountId,
        cardId = cardId,
        amount = amount,
        currency = currency,
        category = SpendingCategory.valueOf(category),
        subcategory = subcategory,
        merchantName = merchantName,
        note = note,
        date = LocalDate.parse(date),
        type = TransactionType.valueOf(type),
        location = if (latitude != null && longitude != null) {
            LocationTag(latitude, longitude, locationName)
        } else null,
        receiptId = receiptId,
        isRecurring = isRecurring,
        createdAt = Instant.parse(createdAt),
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
    )
}
