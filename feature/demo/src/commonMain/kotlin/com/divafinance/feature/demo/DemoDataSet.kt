package com.divafinance.feature.demo

import com.divafinance.core.model.Account
import com.divafinance.core.model.CardRewardRule
import com.divafinance.core.model.CreditCard
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.GraphThreshold
import com.divafinance.core.model.LocationTag
import com.divafinance.core.model.Receipt
import com.divafinance.core.model.Transaction
import com.divafinance.core.model.enums.AccountType
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.CardNetwork
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Every row the demo creates carries this id prefix. It is the only thing that
 * distinguishes demo rows from real ones, so removal can be exact without a schema
 * change — see [DemoDataManager.clear].
 *
 * Nothing else in the app may mint ids with this prefix.
 */
const val DEMO_ID_PREFIX = "demo_"

/**
 * A deterministic, self-consistent snapshot of a plausible few months of spending.
 *
 * The content is chosen so that every screen has something to show: income *and*
 * spending for the dashboard, competing reward rules for the best-card recommendation,
 * enough categories and history for the graphs, geotagged transactions for the map,
 * receipts in each status for the scanner, and all three post types for the feed.
 */
class DemoDataSet(
    val accounts: List<Account>,
    val cards: List<CreditCard>,
    val rewardRules: List<CardRewardRule>,
    val transactions: List<Transaction>,
    val receipts: List<Receipt>,
    val feedPosts: List<FeedPost>,
    val thresholds: List<GraphThreshold>,
)

private const val CHECKING_ID = "${DEMO_ID_PREFIX}account_checking"
private const val SAVINGS_ID = "${DEMO_ID_PREFIX}account_savings"
private const val CARD_TRAVEL_ID = "${DEMO_ID_PREFIX}card_travel"
private const val CARD_CASH_ID = "${DEMO_ID_PREFIX}card_cash"
private const val CARD_MILES_ID = "${DEMO_ID_PREFIX}card_miles"

/** Compact spec for a demo transaction; expanded into [Transaction] below. */
private data class Spend(
    val daysAgo: Long,
    val amount: Double,
    val category: SpendingCategory,
    val merchant: String,
    val cardId: String?,
    val type: TransactionType = TransactionType.DEBIT,
    val note: String? = null,
    val recurring: Boolean = false,
    val location: LocationTag? = null,
)

/**
 * Builds the demo data relative to [today], so the graphs and "recent" lists look
 * current no matter when the demo is switched on.
 */
fun buildDemoDataSet(
    today: LocalDate,
    now: Instant,
    currency: String,
): DemoDataSet {
    val accounts = listOf(
        Account(
            id = CHECKING_ID,
            name = "Everyday Checking",
            type = AccountType.CHECKING,
            currency = currency,
            balance = 4820.55,
            color = "#3B82F6",
            createdAt = now,
            updatedAt = now,
        ),
        Account(
            id = SAVINGS_ID,
            name = "Rainy Day Savings",
            type = AccountType.SAVINGS,
            currency = currency,
            balance = 12500.00,
            color = "#10B981",
            createdAt = now,
            updatedAt = now,
        ),
    )

    val cards = listOf(
        CreditCard(
            id = CARD_TRAVEL_ID,
            accountId = CHECKING_ID,
            name = "Sapphire Traveler",
            lastFour = "4021",
            network = CardNetwork.VISA,
            color = "#1E3A8A",
            creditLimit = 12000.0,
            currentBalance = 2340.18,
            statementDate = 12,
            dueDate = 5,
            annualFee = 95.0,
            createdAt = now,
            updatedAt = now,
        ),
        CreditCard(
            id = CARD_CASH_ID,
            accountId = CHECKING_ID,
            name = "Everyday Cash",
            lastFour = "8890",
            network = CardNetwork.MASTERCARD,
            color = "#B45309",
            creditLimit = 6000.0,
            currentBalance = 812.40,
            statementDate = 20,
            dueDate = 15,
            annualFee = 0.0,
            createdAt = now,
            updatedAt = now,
        ),
        CreditCard(
            id = CARD_MILES_ID,
            accountId = CHECKING_ID,
            name = "Platinum Miles",
            lastFour = "1007",
            network = CardNetwork.AMEX,
            color = "#374151",
            creditLimit = 20000.0,
            currentBalance = 5120.75,
            statementDate = 28,
            dueDate = 22,
            annualFee = 250.0,
            createdAt = now,
            updatedAt = now,
        ),
    )

    // Deliberately overlapping so "best card for category" has a real decision to make:
    // travel is 3x points vs 5x miles, dining 3x points vs 4x miles, and the cash card
    // wins groceries/gas outright.
    val rewardRules = listOf(
        rule("travel_travel", CARD_TRAVEL_ID, SpendingCategory.TRAVEL, 3.0, RewardType.POINTS),
        rule("travel_dining", CARD_TRAVEL_ID, SpendingCategory.DINING, 3.0, RewardType.POINTS),
        rule("travel_base", CARD_TRAVEL_ID, SpendingCategory.OTHER, 1.0, RewardType.POINTS),

        rule(
            "cash_groceries", CARD_CASH_ID, SpendingCategory.GROCERIES, 6.0, RewardType.CASHBACK,
            capAmount = 1500.0, capPeriod = CapPeriod.MONTHLY,
        ),
        rule("cash_gas", CARD_CASH_ID, SpendingCategory.GAS, 3.0, RewardType.CASHBACK),
        rule("cash_base", CARD_CASH_ID, SpendingCategory.OTHER, 1.0, RewardType.CASHBACK),

        rule(
            "miles_travel", CARD_MILES_ID, SpendingCategory.TRAVEL, 5.0, RewardType.MILES,
            capAmount = 10000.0, capPeriod = CapPeriod.ANNUAL,
        ),
        rule("miles_dining", CARD_MILES_ID, SpendingCategory.DINING, 4.0, RewardType.MILES),
        rule("miles_subs", CARD_MILES_ID, SpendingCategory.SUBSCRIPTIONS, 2.0, RewardType.MILES),
    )

    val sanFrancisco = LocationTag(37.7749, -122.4194, "San Francisco, CA")
    val oakland = LocationTag(37.8044, -122.2712, "Oakland, CA")
    val paloAlto = LocationTag(37.4419, -122.1430, "Palo Alto, CA")
    val napa = LocationTag(38.2975, -122.2869, "Napa, CA")
    val seattle = LocationTag(47.6062, -122.3321, "Seattle, WA")

    val spends = listOf(
        // Income — gives the dashboard a non-zero net and the feed a milestone to react to.
        Spend(2, 4200.00, SpendingCategory.OTHER, "Acme Corp Payroll", null, TransactionType.CREDIT, note = "Semi-monthly salary", recurring = true),
        Spend(17, 4200.00, SpendingCategory.OTHER, "Acme Corp Payroll", null, TransactionType.CREDIT, recurring = true),
        Spend(32, 4200.00, SpendingCategory.OTHER, "Acme Corp Payroll", null, TransactionType.CREDIT, recurring = true),
        Spend(47, 4200.00, SpendingCategory.OTHER, "Acme Corp Payroll", null, TransactionType.CREDIT, recurring = true),
        Spend(24, 320.00, SpendingCategory.OTHER, "Refund — Returned Jacket", CARD_CASH_ID, TransactionType.CREDIT),

        // Dining — the category most likely to breach its threshold.
        Spend(1, 68.40, SpendingCategory.DINING, "Blue Fig Bistro", CARD_MILES_ID, note = "Dinner with Sam", location = sanFrancisco),
        Spend(3, 24.15, SpendingCategory.DINING, "Corner Coffee", CARD_TRAVEL_ID, location = sanFrancisco),
        Spend(6, 112.90, SpendingCategory.DINING, "Sakura Omakase", CARD_MILES_ID, note = "Anniversary", location = paloAlto),
        Spend(11, 41.20, SpendingCategory.DINING, "Taqueria Vista", CARD_TRAVEL_ID, location = oakland),
        Spend(19, 87.65, SpendingCategory.DINING, "Harbor Grill", CARD_MILES_ID, location = seattle),
        Spend(28, 33.80, SpendingCategory.DINING, "Corner Coffee", CARD_TRAVEL_ID),
        Spend(44, 96.30, SpendingCategory.DINING, "Blue Fig Bistro", CARD_MILES_ID, location = sanFrancisco),
        Spend(61, 52.10, SpendingCategory.DINING, "Noodle House", CARD_TRAVEL_ID),
        Spend(83, 78.55, SpendingCategory.DINING, "Harbor Grill", CARD_MILES_ID, location = seattle),

        // Groceries — steady weekly rhythm on the cashback card.
        Spend(4, 142.36, SpendingCategory.GROCERIES, "GreenLeaf Market", CARD_CASH_ID, location = sanFrancisco),
        Spend(12, 128.74, SpendingCategory.GROCERIES, "GreenLeaf Market", CARD_CASH_ID),
        Spend(20, 156.92, SpendingCategory.GROCERIES, "GreenLeaf Market", CARD_CASH_ID),
        Spend(27, 119.48, SpendingCategory.GROCERIES, "Sunrise Grocers", CARD_CASH_ID, location = oakland),
        Spend(41, 163.05, SpendingCategory.GROCERIES, "GreenLeaf Market", CARD_CASH_ID),
        Spend(55, 134.20, SpendingCategory.GROCERIES, "Sunrise Grocers", CARD_CASH_ID),
        Spend(76, 148.66, SpendingCategory.GROCERIES, "GreenLeaf Market", CARD_CASH_ID),

        // Travel — the big-ticket items that dominate the pie chart.
        Spend(9, 642.00, SpendingCategory.TRAVEL, "Skyline Airways", CARD_MILES_ID, note = "SFO → SEA return"),
        Spend(21, 318.75, SpendingCategory.TRAVEL, "Harbor View Hotel", CARD_MILES_ID, location = seattle),
        Spend(38, 1240.00, SpendingCategory.TRAVEL, "Skyline Airways", CARD_MILES_ID, note = "Holiday flights"),
        Spend(66, 289.40, SpendingCategory.TRAVEL, "Vineyard Inn", CARD_TRAVEL_ID, location = napa),

        // Gas & transportation.
        Spend(5, 58.22, SpendingCategory.GAS, "Petro Station 14", CARD_CASH_ID),
        Spend(18, 61.05, SpendingCategory.GAS, "Petro Station 14", CARD_CASH_ID),
        Spend(36, 54.90, SpendingCategory.GAS, "QuickFuel", CARD_CASH_ID, location = oakland),
        Spend(7, 23.50, SpendingCategory.TRANSPORTATION, "Metro Transit", CARD_TRAVEL_ID, recurring = true),
        Spend(30, 23.50, SpendingCategory.TRANSPORTATION, "Metro Transit", CARD_TRAVEL_ID, recurring = true),
        Spend(14, 42.80, SpendingCategory.TRANSPORTATION, "RideShare", CARD_TRAVEL_ID, location = sanFrancisco),

        // Subscriptions — all recurring, so the recurring filter has content.
        Spend(8, 15.99, SpendingCategory.SUBSCRIPTIONS, "Streamly", CARD_MILES_ID, recurring = true),
        Spend(8, 10.99, SpendingCategory.SUBSCRIPTIONS, "Tunebox", CARD_MILES_ID, recurring = true),
        Spend(13, 52.00, SpendingCategory.SUBSCRIPTIONS, "Cloud Storage Pro", CARD_MILES_ID, recurring = true),
        Spend(38, 15.99, SpendingCategory.SUBSCRIPTIONS, "Streamly", CARD_MILES_ID, recurring = true),

        // Shopping.
        Spend(10, 189.99, SpendingCategory.SHOPPING, "Northline Outfitters", CARD_CASH_ID, location = sanFrancisco),
        Spend(26, 74.25, SpendingCategory.SHOPPING, "Paper & Pen Co.", CARD_CASH_ID),
        Spend(49, 512.00, SpendingCategory.SHOPPING, "Techmart", CARD_TRAVEL_ID, note = "New monitor"),

        // Utilities, healthcare, entertainment, education — round out the categories.
        Spend(15, 128.44, SpendingCategory.UTILITIES, "City Power & Water", null, recurring = true),
        Spend(45, 134.10, SpendingCategory.UTILITIES, "City Power & Water", null, recurring = true),
        Spend(22, 45.00, SpendingCategory.HEALTHCARE, "Bayside Pharmacy", CARD_CASH_ID),
        Spend(52, 220.00, SpendingCategory.HEALTHCARE, "Dr. Nguyen — Checkup", CARD_CASH_ID),
        Spend(16, 62.00, SpendingCategory.ENTERTAINMENT, "Grand Cinema", CARD_TRAVEL_ID, location = sanFrancisco),
        Spend(34, 145.00, SpendingCategory.ENTERTAINMENT, "Riverside Concerts", CARD_MILES_ID, location = oakland),
        Spend(29, 320.00, SpendingCategory.EDUCATION, "Kotlin Deep Dive Course", CARD_TRAVEL_ID, note = "Professional development"),
    )

    val transactions = spends.mapIndexed { index, spend ->
        Transaction(
            id = "${DEMO_ID_PREFIX}txn_$index",
            accountId = CHECKING_ID,
            cardId = spend.cardId,
            amount = spend.amount,
            currency = currency,
            category = spend.category,
            merchantName = spend.merchant,
            note = spend.note,
            date = today.minusDays(spend.daysAgo),
            type = spend.type,
            location = spend.location,
            // Wired up below for the one transaction that has a matching receipt.
            receiptId = null,
            isRecurring = spend.recurring,
            createdAt = now,
        )
    }

    // Link the processed receipt to a real transaction so the scanner's "matched"
    // path has something to show.
    val receiptTarget = transactions.first { it.merchantName == "GreenLeaf Market" }
    val receipts = listOf(
        Receipt(
            id = "${DEMO_ID_PREFIX}receipt_matched",
            transactionId = receiptTarget.id,
            ocrText = "GREENLEAF MARKET\n12 items\nTOTAL ${receiptTarget.amount}",
            merchantName = "GreenLeaf Market",
            totalAmount = receiptTarget.amount,
            date = receiptTarget.date,
            status = ReceiptStatus.PROCESSED,
            createdAt = now,
        ),
        Receipt(
            id = "${DEMO_ID_PREFIX}receipt_pending",
            ocrText = "CORNER COFFEE\nLATTE 5.40\nTOTAL 5.40",
            merchantName = "Corner Coffee",
            totalAmount = 5.40,
            date = today.minusDays(1),
            status = ReceiptStatus.PENDING,
            createdAt = now,
        ),
        Receipt(
            id = "${DEMO_ID_PREFIX}receipt_failed",
            ocrText = ";;;unreadable;;;",
            status = ReceiptStatus.FAILED,
            createdAt = now,
        ),
    )

    val transactionsWithReceipt = transactions.map {
        if (it.id == receiptTarget.id) it.copy(receiptId = "${DEMO_ID_PREFIX}receipt_matched") else it
    }

    val feedPosts = listOf(
        FeedPost(
            id = "${DEMO_ID_PREFIX}feed_insight",
            type = FeedPostType.BOT_INSIGHT,
            title = "Dining is trending up",
            body = "You spent 18% more on dining this month than last. Your Platinum Miles card " +
                "earns 4x on dining — using it for the next few meals would recover some of that.",
            createdAt = now,
        ),
        FeedPost(
            id = "${DEMO_ID_PREFIX}feed_milestone",
            type = FeedPostType.MILESTONE,
            title = "Savings goal 62% funded",
            body = "Rainy Day Savings crossed $12,500. At your current pace you'll hit the " +
                "$20,000 target in about seven months.",
            createdAt = now,
        ),
        FeedPost(
            id = "${DEMO_ID_PREFIX}feed_txn",
            type = FeedPostType.TRANSACTION,
            title = "Large purchase logged",
            body = "Skyline Airways — 1,240.00 on Platinum Miles. That's 6,200 miles earned.",
            transactionId = transactions.first { it.merchantName == "Skyline Airways" }.id,
            createdAt = now,
        ),
        FeedPost(
            id = "${DEMO_ID_PREFIX}feed_insight_cards",
            type = FeedPostType.BOT_INSIGHT,
            title = "You're leaving cashback on the table",
            body = "Three grocery runs went on a 1% card this month. Everyday Cash earns 6% on " +
                "groceries up to 1,500 per month.",
            createdAt = now,
        ),
    )

    // Chosen so at least one category sits over its limit and one comfortably under,
    // giving the threshold chart both colours.
    val thresholds = listOf(
        threshold("dining", SpendingCategory.DINING, 12.0),
        threshold("travel", SpendingCategory.TRAVEL, 30.0),
        threshold("groceries", SpendingCategory.GROCERIES, 18.0),
        threshold("shopping", SpendingCategory.SHOPPING, 15.0),
    )

    return DemoDataSet(
        accounts = accounts,
        cards = cards,
        rewardRules = rewardRules,
        transactions = transactionsWithReceipt,
        receipts = receipts,
        feedPosts = feedPosts,
        thresholds = thresholds,
    )
}

private fun rule(
    suffix: String,
    cardId: String,
    category: SpendingCategory,
    multiplier: Double,
    rewardType: RewardType,
    capAmount: Double? = null,
    capPeriod: CapPeriod? = null,
) = CardRewardRule(
    id = "${DEMO_ID_PREFIX}rule_$suffix",
    cardId = cardId,
    category = category,
    multiplier = multiplier,
    rewardType = rewardType,
    capAmount = capAmount,
    capPeriod = capPeriod,
)

private fun threshold(suffix: String, category: SpendingCategory, percent: Double) =
    GraphThreshold(
        id = "${DEMO_ID_PREFIX}threshold_$suffix",
        category = category,
        thresholdPercent = percent,
    )

private fun LocalDate.minusDays(days: Long): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() - days)
