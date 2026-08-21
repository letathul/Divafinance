package com.divafinance.feature.demo

import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class DemoDataManagerTest {

    private val settings = FakeSettingsRepository()
    private val accounts = FakeAccountRepository()
    private val cards = FakeCardRepository()
    private val rewards = FakeRewardRepository()
    private val transactions = FakeTransactionRepository()
    private val receipts = FakeReceiptRepository()
    private val feed = FakeFeedRepository()
    private val thresholds = FakeThresholdRepository()
    private val people = FakePersonRepository()
    private val ledger = FakeLedgerRepository()

    private var installs = 0
    private var uninstalls = 0
    private var installSucceeds = true

    private val installer = object : DemoModuleInstaller {
        override suspend fun install(onProgress: (Float) -> Unit): Boolean {
            installs++
            return installSucceeds
        }

        override suspend fun uninstall(): Boolean {
            uninstalls++
            return true
        }
    }

    private val manager = DemoDataManager(
        settingsRepository = settings,
        accountRepository = accounts,
        cardRepository = cards,
        rewardRepository = rewards,
        transactionRepository = transactions,
        receiptRepository = receipts,
        feedRepository = feed,
        thresholdRepository = thresholds,
        personRepository = people,
        ledgerRepository = ledger,
        moduleInstaller = installer,
    )

    @Test
    fun offerStartsAvailable() = runTest {
        assertEquals(DemoStatus.UNDECIDED, manager.status())
        assertTrue(manager.isOfferAvailable())
    }

    @Test
    fun seedPopulatesEveryFeatureArea() = runTest {
        assertTrue(manager.seed("USD"))

        assertEquals(DemoStatus.ACTIVE, manager.status())
        assertEquals(1, installs)

        assertTrue(accounts.items.value.isNotEmpty())
        assertTrue(cards.items.value.size >= 3)
        assertTrue(rewards.items.value.isNotEmpty())
        assertTrue(transactions.items.value.size >= 40)
        assertTrue(receipts.items.value.isNotEmpty())
        assertTrue(feed.items.value.isNotEmpty())
        assertTrue(thresholds.items.value.isNotEmpty())

        // The map, recurring filter and income totals all need their own slice of data.
        assertTrue(transactions.items.value.any { it.location != null })
        assertTrue(transactions.items.value.any { it.isRecurring })
        assertTrue(transactions.items.value.any { it.type == TransactionType.CREDIT })
        // Every category should appear so the graphs are fully populated.
        assertEquals(
            SpendingCategory.entries.toSet(),
            transactions.items.value.map { it.category }.toSet(),
        )
    }

    @Test
    fun everySeededRowCarriesTheDemoPrefix() = runTest {
        manager.seed("USD")

        val ids = accounts.items.value.map { it.id } +
            cards.items.value.map { it.id } +
            rewards.items.value.map { it.id } +
            transactions.items.value.map { it.id } +
            receipts.items.value.map { it.id } +
            feed.items.value.map { it.id } +
            thresholds.items.value.map { it.id }

        assertTrue(ids.isNotEmpty())
        assertTrue(ids.all { it.startsWith(DEMO_ID_PREFIX) }, "unprefixed demo ids: $ids")
    }

    @Test
    fun clearRemovesEverythingAndRetiresTheOffer() = runTest {
        manager.seed("USD")
        manager.clear()

        assertEquals(DemoStatus.RETIRED, manager.status())
        assertFalse(manager.isOfferAvailable())
        assertEquals(1, uninstalls)

        assertTrue(accounts.items.value.isEmpty())
        assertTrue(cards.items.value.isEmpty())
        assertTrue(rewards.items.value.isEmpty())
        assertTrue(transactions.items.value.isEmpty())
        assertTrue(receipts.items.value.isEmpty())
        assertTrue(feed.items.value.isEmpty())
        assertTrue(thresholds.items.value.isEmpty())
    }

    @Test
    fun clearKeepsUserTransactionsAndUnlinksDemoCards() = runTest {
        manager.seed("USD")

        val demoCardId = cards.items.value.first().id
        val userTransaction = Transaction(
            id = "user_txn_1",
            accountId = "user_account",
            cardId = demoCardId,
            amount = 12.0,
            category = SpendingCategory.OTHER,
            date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            type = TransactionType.DEBIT,
            createdAt = Clock.System.now(),
        )
        transactions.insert(userTransaction)

        manager.clear()

        val survivor = transactions.items.value.singleOrNull { it.id == "user_txn_1" }
        assertTrue(survivor != null, "user transaction must survive demo removal")
        assertNull(survivor.cardId, "reference to the deleted demo card must be cleared")
    }

    @Test
    fun declineRetiresWithoutSeedingAnything() = runTest {
        manager.decline()

        assertEquals(DemoStatus.RETIRED, manager.status())
        assertFalse(manager.isOfferAvailable())
        assertEquals(0, installs)
        assertTrue(transactions.items.value.isEmpty())
    }

    @Test
    fun seedIsRefusedOnceTheOfferIsRetired() = runTest {
        manager.decline()

        assertFalse(manager.seed("USD"))
        assertEquals(DemoStatus.RETIRED, manager.status())
        assertTrue(transactions.items.value.isEmpty())
    }

    @Test
    fun demoCannotBeSeededTwice() = runTest {
        assertTrue(manager.seed("USD"))
        val countAfterFirst = transactions.items.value.size

        assertFalse(manager.seed("USD"))
        assertEquals(countAfterFirst, transactions.items.value.size)
    }

    @Test
    fun demoStillLoadsWhenTheOnDemandModuleCannotBeDelivered() = runTest {
        // Sideloaded debug builds and offline devices get no split. Only the optional
        // tour screen lives there, so the sample data must load anyway.
        installSucceeds = false

        assertTrue(manager.seed("USD"))

        assertEquals(DemoStatus.ACTIVE, manager.status())
        assertTrue(transactions.items.value.isNotEmpty())
    }

    @Test
    fun seedsPeopleAndDebts() = runTest {
        manager.seed("USD")

        assertEquals(setOf("Sam", "Alex", "Priya"), people.items.value.map { it.name }.toSet())
        assertTrue(ledger.items.value.isNotEmpty())
    }

    /**
     * The demo has to satisfy the same invariant real data does: a split transaction's
     * othersShare equals the sum of the ledger entries pointing at it. If the fixture
     * drifts, every screen reading either side shows a different number.
     */
    @Test
    fun theDemoSplitReconciles() = runTest {
        manager.seed("USD")

        val split = transactions.items.value.single { it.othersShare > 0.0 }
        val owed = ledger.items.value
            .filter { it.transactionId == split.id }
            .sumOf { it.amount }

        assertEquals(split.othersShare, owed)
        assertTrue(split.othersShare < split.amount, "a split cannot owe more than it cost")
    }

    @Test
    fun clearingRemovesPeopleAndTheirDebts() = runTest {
        manager.seed("USD")
        manager.clear()

        assertTrue(people.items.value.isEmpty())
        assertTrue(ledger.items.value.isEmpty())
    }
}
