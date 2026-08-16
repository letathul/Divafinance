package com.divafinance.feature.demo

import com.divafinance.core.data.repository.AccountRepository
import com.divafinance.core.data.repository.CardRepository
import com.divafinance.core.data.repository.FeedRepository
import com.divafinance.core.data.repository.LedgerRepository
import com.divafinance.core.data.repository.PersonRepository
import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.data.repository.RewardRepository
import com.divafinance.core.data.repository.SettingsRepository
import com.divafinance.core.data.repository.ThresholdRepository
import com.divafinance.core.data.repository.TransactionRepository
import com.divafinance.core.model.Account
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.LedgerEntry
import com.divafinance.core.model.Person
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.model.enums.SpendingCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Minimal in-memory doubles; only what DemoDataManager actually touches. */

class FakeSettingsRepository : SettingsRepository {
    private val values = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun getAll(): Flow<List<UserSettings>> =
        values.map { m -> m.entries.map { UserSettings(it.key, it.value) } }
    override suspend fun get(key: String): String? = values.value[key]
    override suspend fun set(key: String, value: String) {
        values.value = values.value + (key to value)
    }
    override suspend fun delete(key: String) { values.value = values.value - key }
    override suspend fun deleteAll() { values.value = emptyMap() }
}

class FakeAccountRepository : AccountRepository {
    val items = MutableStateFlow<List<Account>>(emptyList())
    override fun getAll(): Flow<List<Account>> = items
    override suspend fun getById(id: String): Account? = items.value.find { it.id == id }
    override suspend fun insert(account: Account) { items.value = items.value + account }
    override suspend fun update(account: Account) {
        items.value = items.value.map { if (it.id == account.id) account else it }
    }
    override suspend fun updateBalance(id: String, balance: Double) {}
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun count(): Long = items.value.size.toLong()
}

class FakeCardRepository : CardRepository {
    val items = MutableStateFlow<List<CreditCard>>(emptyList())
    override fun getAll(): Flow<List<CreditCard>> = items
    override suspend fun getById(id: String): CreditCard? = items.value.find { it.id == id }
    override suspend fun getByAccountId(accountId: String): List<CreditCard> =
        items.value.filter { it.accountId == accountId }
    override suspend fun insert(card: CreditCard) { items.value = items.value + card }
    override suspend fun update(card: CreditCard) {
        items.value = items.value.map { if (it.id == card.id) card else it }
    }
    override suspend fun updateBalance(id: String, balance: Double) {}
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun count(): Long = items.value.size.toLong()
}

class FakeRewardRepository : RewardRepository {
    val items = MutableStateFlow<List<CardRewardRule>>(emptyList())
    override fun getAll(): Flow<List<CardRewardRule>> = items
    override suspend fun getByCardId(cardId: String) = items.value.filter { it.cardId == cardId }
    override suspend fun getByCategory(category: SpendingCategory) =
        items.value.filter { it.category == category }
    override suspend fun insert(rule: CardRewardRule) { items.value = items.value + rule }
    override suspend fun update(rule: CardRewardRule) {
        items.value = items.value.map { if (it.id == rule.id) rule else it }
    }
    override suspend fun deleteByCardId(cardId: String) {
        items.value = items.value.filterNot { it.cardId == cardId }
    }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
}

class FakeTransactionRepository : TransactionRepository {
    val items = MutableStateFlow<List<Transaction>>(emptyList())
    override fun getAll(): Flow<List<Transaction>> = items
    override suspend fun getById(id: String) = items.value.find { it.id == id }
    override suspend fun getByAccountId(accountId: String) =
        items.value.filter { it.accountId == accountId }
    override suspend fun getByCategory(category: String) = emptyList<Transaction>()
    override suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate) =
        emptyList<Transaction>()
    override suspend fun getByCategoryAndDateRange(
        category: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = emptyList<Transaction>()
    override suspend fun getWithLocation() = items.value.filter { it.location != null }
    override suspend fun getKnownMerchants() =
        items.value.mapNotNull { it.merchantName }.distinct()
    override suspend fun getSpendingByCategory(startDate: LocalDate, endDate: LocalDate) =
        emptyMap<String, Double>()
    override suspend fun getTotalSpending(startDate: LocalDate, endDate: LocalDate): Double? = null
    override suspend fun insert(transaction: Transaction) { items.value = items.value + transaction }
    override suspend fun update(transaction: Transaction) {
        items.value = items.value.map { if (it.id == transaction.id) transaction else it }
    }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun count(): Long = items.value.size.toLong()
}

class FakeReceiptRepository : ReceiptRepository {
    val items = MutableStateFlow<List<Receipt>>(emptyList())
    override fun getAll(): Flow<List<Receipt>> = items
    override suspend fun getById(id: String) = items.value.find { it.id == id }
    override suspend fun getByTransactionId(transactionId: String) =
        items.value.find { it.transactionId == transactionId }
    override suspend fun getByStatus(status: ReceiptStatus) =
        items.value.filter { it.status == status }
    override suspend fun insert(receipt: Receipt) { items.value = items.value + receipt }
    override suspend fun update(receipt: Receipt) {
        items.value = items.value.map { if (it.id == receipt.id) receipt else it }
    }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
}

class FakeFeedRepository : FeedRepository {
    val items = MutableStateFlow<List<FeedPost>>(emptyList())
    override fun getAll(): Flow<List<FeedPost>> = items
    override suspend fun getRecent(limit: Long) = items.value.take(limit.toInt())
    override suspend fun getByType(type: FeedPostType) = items.value.filter { it.type == type }
    override suspend fun getLatestBotInsight() =
        items.value.firstOrNull { it.type == FeedPostType.BOT_INSIGHT }
    override suspend fun insert(post: FeedPost) { items.value = items.value + post }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
}

class FakeThresholdRepository : ThresholdRepository {
    val items = MutableStateFlow<List<GraphThreshold>>(emptyList())
    override fun getAll(): Flow<List<GraphThreshold>> = items
    override suspend fun getByCategory(category: SpendingCategory) =
        items.value.find { it.category == category }
    override suspend fun insert(threshold: GraphThreshold) { items.value = items.value + threshold }
    override suspend fun update(threshold: GraphThreshold) {
        items.value = items.value.map { if (it.id == threshold.id) threshold else it }
    }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
}

class FakePersonRepository : PersonRepository {
    val items = MutableStateFlow<List<Person>>(emptyList())
    override fun getAll(): Flow<List<Person>> = items
    override fun getActive(): Flow<List<Person>> = items.map { list -> list.filterNot { it.isArchived } }
    override suspend fun getById(id: String) = items.value.find { it.id == id }
    override suspend fun findByName(name: String) =
        items.value.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
    override suspend fun insert(person: Person) { items.value = items.value + person }
    override suspend fun update(person: Person) {
        items.value = items.value.map { if (it.id == person.id) person else it }
    }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun count(): Long = items.value.size.toLong()
}

class FakeLedgerRepository : LedgerRepository {
    val items = MutableStateFlow<List<LedgerEntry>>(emptyList())
    override fun getAll(): Flow<List<LedgerEntry>> = items
    override suspend fun getByPersonId(personId: String) = items.value.filter { it.personId == personId }
    override suspend fun getByTransactionId(transactionId: String) =
        items.value.filter { it.transactionId == transactionId }
    override suspend fun insert(entry: LedgerEntry) { items.value = items.value + entry }
    override suspend fun update(entry: LedgerEntry) {
        items.value = items.value.map { if (it.id == entry.id) entry else it }
    }
    override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
    override suspend fun deleteByTransactionId(transactionId: String) {
        items.value = items.value.filterNot { it.transactionId == transactionId }
    }
    override suspend fun deleteByPersonId(personId: String) {
        items.value = items.value.filterNot { it.personId == personId }
    }
    override suspend fun count(): Long = items.value.size.toLong()
}
