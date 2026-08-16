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
import com.divafinance.core.model.UserSettings
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Owns the whole life of the demo: seeding it, tearing it down, and recording the
 * one-way status that decides whether the offer is ever shown again.
 *
 * Both exits ([decline] and [clear]) land on [DemoStatus.RETIRED], and nothing here
 * writes [DemoStatus.UNDECIDED], so the offer cannot reappear once answered.
 */
class DemoDataManager(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val rewardRepository: RewardRepository,
    private val transactionRepository: TransactionRepository,
    private val receiptRepository: ReceiptRepository,
    private val feedRepository: FeedRepository,
    private val thresholdRepository: ThresholdRepository,
    private val personRepository: PersonRepository,
    private val ledgerRepository: LedgerRepository,
    private val moduleInstaller: DemoModuleInstaller = DemoModuleInstaller.NoOp,
) {

    suspend fun status(): DemoStatus =
        DemoStatus.fromStorage(settingsRepository.get(UserSettings.KEY_DEMO_STATUS))

    /** True only while no decision has been recorded. */
    suspend fun isOfferAvailable(): Boolean = status() == DemoStatus.UNDECIDED

    /** Records a "no thanks" at onboarding. Terminal — the offer is gone for good. */
    suspend fun decline() {
        setStatus(DemoStatus.RETIRED)
    }

    /**
     * Seeds the demo data set and marks the demo active. No-op unless the offer is
     * still open, so a repeat call can't duplicate rows or resurrect a retired demo.
     *
     * Returns false only if the offer was already answered.
     *
     * Delivery of the on-demand module is best-effort: it carries the optional demo tour
     * screen, but the sample data itself lives in the base app. A sideloaded debug build
     * or an offline device gets no split, and the demo must still work there — so a
     * failed install is not allowed to block seeding.
     */
    suspend fun seed(currency: String, onProgress: (Float) -> Unit = {}): Boolean {
        if (status() != DemoStatus.UNDECIDED) return false

        moduleInstaller.install(onProgress)

        val now = Clock.System.now()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val data = buildDemoDataSet(today = today, now = now, currency = currency)

        // Insert parents before children so nothing references a missing row.
        data.accounts.forEach { accountRepository.insert(it) }
        data.cards.forEach { cardRepository.insert(it) }
        data.rewardRules.forEach { rewardRepository.insert(it) }
        data.transactions.forEach { transactionRepository.insert(it) }
        data.receipts.forEach { receiptRepository.insert(it) }
        data.feedPosts.forEach { feedRepository.insert(it) }
        data.thresholds.forEach { thresholdRepository.insert(it) }
        // People before entries: an entry references a person and a transaction.
        data.people.forEach { personRepository.insert(it) }
        data.ledgerEntries.forEach { ledgerRepository.insert(it) }

        setStatus(DemoStatus.ACTIVE)
        return true
    }

    /**
     * Removes every row the demo created and retires the offer permanently.
     *
     * Demo rows are identified purely by [DEMO_ID_PREFIX]. Anything the user created
     * themselves is left alone, with one unavoidable exception: a user transaction
     * booked against a demo *account* cannot survive its account, so it is removed
     * too. A user transaction that merely used a demo *card* is kept, with its card
     * reference cleared. The UI warns about this before calling.
     */
    suspend fun clear() {
        val demoAccountIds = accountRepository.getAll().first()
            .map { it.id }.filter { it.isDemo() }.toSet()
        val demoCardIds = cardRepository.getAll().first()
            .map { it.id }.filter { it.isDemo() }.toSet()

        // Ledger entries reference both people and transactions, so they go first of all.
        // Entries the user added against a demo person are swept too: without the person
        // there is no balance for them to belong to.
        val demoPersonIds = personRepository.getAll().first()
            .map { it.id }.filter { it.isDemo() }.toSet()

        ledgerRepository.getAll().first()
            .filter { it.id.isDemo() || it.personId in demoPersonIds || it.transactionId?.isDemo() == true }
            .forEach { ledgerRepository.delete(it.id) }

        demoPersonIds.forEach { personRepository.delete(it) }

        // Children first: feed posts and receipts point at transactions.
        // Insights the app generated *about* demo spending are swept too — they are not
        // demo-prefixed, but they only describe numbers that are about to disappear.
        feedRepository.getAll().first()
            .filter { it.id.isDemo() || it.transactionId?.isDemo() == true }
            .forEach { feedRepository.delete(it.id) }

        receiptRepository.getAll().first()
            .filter { it.id.isDemo() }
            .forEach { receiptRepository.delete(it.id) }

        transactionRepository.getAll().first().forEach { transaction ->
            when {
                transaction.id.isDemo() || transaction.accountId in demoAccountIds ->
                    transactionRepository.delete(transaction.id)

                transaction.cardId in demoCardIds ->
                    transactionRepository.update(transaction.copy(cardId = null))
            }
        }

        rewardRepository.getAll().first()
            .filter { it.id.isDemo() || it.cardId in demoCardIds }
            .forEach { rewardRepository.delete(it.id) }

        demoCardIds.forEach { cardRepository.delete(it) }
        demoAccountIds.forEach { accountRepository.delete(it) }

        thresholdRepository.getAll().first()
            .filter { it.id.isDemo() }
            .forEach { thresholdRepository.delete(it.id) }

        // Retire first: the data is already gone, so the offer must not come back even
        // if the platform declines the uninstall.
        setStatus(DemoStatus.RETIRED)
        moduleInstaller.uninstall()
    }

    private suspend fun setStatus(status: DemoStatus) {
        settingsRepository.set(UserSettings.KEY_DEMO_STATUS, status.name)
    }

    private fun String.isDemo() = startsWith(DEMO_ID_PREFIX)
}
