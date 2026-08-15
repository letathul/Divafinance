package com.divafinance.core.data.mapper

import com.divafinance.core.model.Account
import com.divafinance.core.model.enums.AccountType
import kotlinx.datetime.Instant

object AccountMapper {

    fun toDomain(
        id: String,
        name: String,
        type: String,
        currency: String,
        balance: Double,
        color: String?,
        icon: String?,
        isActive: Boolean,
        createdAt: String,
        updatedAt: String,
    ): Account = Account(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        currency = currency,
        balance = balance,
        color = color,
        icon = icon,
        isActive = isActive,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

    fun toMap(account: Account): Map<String, Any?> = mapOf(
        "id" to account.id,
        "name" to account.name,
        "type" to account.type.name,
        "currency" to account.currency,
        "balance" to account.balance,
        "color" to account.color,
        "icon" to account.icon,
        "is_active" to account.isActive,
        "created_at" to account.createdAt.toString(),
        "updated_at" to account.updatedAt.toString(),
    )
}
