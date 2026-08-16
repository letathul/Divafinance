package com.divafinance.core.testing.fake

import com.divafinance.core.model.*
import com.divafinance.core.model.enums.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

object TestData {
    val now = Clock.System.now()

    fun card(
        id: String = "card-1",
        accountId: String = "acc-1",
        name: String = "Test Card",
        creditLimit: Double = 5000.0,
        currentBalance: Double = 1000.0,
        rewardRules: List<CardRewardRule> = emptyList(),
        isActive: Boolean = true,
    ) = CreditCard(
        id = id,
        accountId = accountId,
        name = name,
        lastFour = "1234",
        network = CardNetwork.VISA,
        creditLimit = creditLimit,
        currentBalance = currentBalance,
        rewardRules = rewardRules,
        isActive = isActive,
        createdAt = now,
        updatedAt = now,
    )

    fun account(
        id: String = "acc-1",
        name: String = "Test Account",
        type: AccountType = AccountType.CHECKING,
        currency: String = "USD",
        balance: Double = 0.0,
    ) = Account(
        id = id,
        name = name,
        type = type,
        currency = currency,
        balance = balance,
        createdAt = now,
        updatedAt = now,
    )

    fun rule(
        id: String = "rule-1",
        cardId: String = "card-1",
        category: SpendingCategory = SpendingCategory.DINING,
        multiplier: Double = 3.0,
        rewardType: RewardType = RewardType.POINTS,
        capAmount: Double? = null,
    ) = CardRewardRule(
        id = id,
        cardId = cardId,
        category = category,
        multiplier = multiplier,
        rewardType = rewardType,
        capAmount = capAmount,
    )

    fun transaction(
        id: String = "tx-1",
        accountId: String = "acc-1",
        cardId: String? = "card-1",
        amount: Double = 50.0,
        category: SpendingCategory = SpendingCategory.DINING,
        type: TransactionType = TransactionType.DEBIT,
        date: LocalDate = LocalDate(2024, 6, 15),
        merchantName: String? = "Test Merchant",
        location: LocationTag? = null,
        othersShare: Double = 0.0,
    ) = Transaction(
        id = id,
        accountId = accountId,
        cardId = cardId,
        amount = amount,
        category = category,
        merchantName = merchantName,
        date = date,
        type = type,
        location = location,
        othersShare = othersShare,
        createdAt = now,
    )

    fun person(
        id: String = "person-1",
        name: String = "Sam",
        note: String? = null,
        isArchived: Boolean = false,
    ) = Person(
        id = id,
        name = name,
        note = note,
        isArchived = isArchived,
        createdAt = now,
        updatedAt = now,
    )

    fun ledgerEntry(
        id: String = "ledger-1",
        personId: String = "person-1",
        amount: Double = 25.0,
        kind: LedgerEntryKind = LedgerEntryKind.LENT,
        note: String? = null,
        date: LocalDate = LocalDate(2024, 6, 15),
        transactionId: String? = null,
    ) = LedgerEntry(
        id = id,
        personId = personId,
        amount = amount,
        kind = kind,
        note = note,
        date = date,
        transactionId = transactionId,
        createdAt = now,
    )

    fun threshold(
        id: String = "thresh-1",
        category: SpendingCategory = SpendingCategory.DINING,
        thresholdPercent: Double = 30.0,
    ) = GraphThreshold(
        id = id,
        category = category,
        thresholdPercent = thresholdPercent,
    )
}
