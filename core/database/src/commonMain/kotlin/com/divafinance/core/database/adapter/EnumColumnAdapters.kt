package com.divafinance.core.database.adapter

import app.cash.sqldelight.ColumnAdapter
import com.divafinance.core.model.enums.AccountType
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.CardNetwork
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType

val accountTypeAdapter = object : ColumnAdapter<AccountType, String> {
    override fun decode(databaseValue: String): AccountType =
        AccountType.valueOf(databaseValue)
    override fun encode(value: AccountType): String = value.name
}

val cardNetworkAdapter = object : ColumnAdapter<CardNetwork, String> {
    override fun decode(databaseValue: String): CardNetwork =
        CardNetwork.valueOf(databaseValue)
    override fun encode(value: CardNetwork): String = value.name
}

val spendingCategoryAdapter = object : ColumnAdapter<SpendingCategory, String> {
    override fun decode(databaseValue: String): SpendingCategory =
        SpendingCategory.valueOf(databaseValue)
    override fun encode(value: SpendingCategory): String = value.name
}

val rewardTypeAdapter = object : ColumnAdapter<RewardType, String> {
    override fun decode(databaseValue: String): RewardType =
        RewardType.valueOf(databaseValue)
    override fun encode(value: RewardType): String = value.name
}

val transactionTypeAdapter = object : ColumnAdapter<TransactionType, String> {
    override fun decode(databaseValue: String): TransactionType =
        TransactionType.valueOf(databaseValue)
    override fun encode(value: TransactionType): String = value.name
}

val capPeriodAdapter = object : ColumnAdapter<CapPeriod, String> {
    override fun decode(databaseValue: String): CapPeriod =
        CapPeriod.valueOf(databaseValue)
    override fun encode(value: CapPeriod): String = value.name
}

val feedPostTypeAdapter = object : ColumnAdapter<FeedPostType, String> {
    override fun decode(databaseValue: String): FeedPostType =
        FeedPostType.valueOf(databaseValue)
    override fun encode(value: FeedPostType): String = value.name
}

val receiptStatusAdapter = object : ColumnAdapter<ReceiptStatus, String> {
    override fun decode(databaseValue: String): ReceiptStatus =
        ReceiptStatus.valueOf(databaseValue)
    override fun encode(value: ReceiptStatus): String = value.name
}
