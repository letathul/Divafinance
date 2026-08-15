package com.divafinance.core.data.mapper

import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.enums.CardNetwork
import kotlinx.datetime.Instant

object CardMapper {

    fun toDomain(
        id: String,
        accountId: String,
        name: String,
        lastFour: String?,
        network: String,
        color: String,
        creditLimit: Double,
        currentBalance: Double,
        statementDate: Int?,
        dueDate: Int?,
        annualFee: Double,
        isActive: Boolean,
        createdAt: String,
        updatedAt: String,
    ): CreditCard = CreditCard(
        id = id,
        accountId = accountId,
        name = name,
        lastFour = lastFour,
        network = CardNetwork.valueOf(network),
        color = color,
        creditLimit = creditLimit,
        currentBalance = currentBalance,
        statementDate = statementDate,
        dueDate = dueDate,
        annualFee = annualFee,
        isActive = isActive,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

    fun toMap(card: CreditCard): Map<String, Any?> = mapOf(
        "id" to card.id,
        "account_id" to card.accountId,
        "name" to card.name,
        "last_four" to card.lastFour,
        "network" to card.network.name,
        "color" to card.color,
        "credit_limit" to card.creditLimit,
        "current_balance" to card.currentBalance,
        "statement_date" to card.statementDate,
        "due_date" to card.dueDate,
        "annual_fee" to card.annualFee,
        "is_active" to card.isActive,
        "created_at" to card.createdAt.toString(),
        "updated_at" to card.updatedAt.toString(),
    )
}
