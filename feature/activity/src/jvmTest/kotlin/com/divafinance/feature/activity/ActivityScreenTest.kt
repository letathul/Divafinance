package com.divafinance.feature.activity

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.activity.ActivityItem
import com.divafinance.core.domain.usecase.activity.ActivityKind
import com.divafinance.core.domain.usecase.people.PersonBalance
import com.divafinance.core.model.FeedPost
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.LedgerEntryKind
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ActivityScreenTest {

    private fun spend(id: String = "t1", merchant: String = "Blue Bottle", othersShare: Double = 0.0) =
        ActivityItem.Spend(
            TestData.transaction(id = id, merchantName = merchant, amount = 120.0, othersShare = othersShare)
        )

    private fun debt(id: String = "l1", name: String = "Sam", kind: LedgerEntryKind = LedgerEntryKind.LENT) =
        ActivityItem.Debt(
            entry = TestData.ledgerEntry(id = id, personId = "p1", amount = 40.0, kind = kind),
            person = TestData.person(id = "p1", name = name),
        )

    private fun insight(id: String = "f1", title: String = "Quiet week") =
        ActivityItem.Insight(
            FeedPost(id = id, type = FeedPostType.BOT_INSIGHT, title = title, body = "Body", createdAt = TestData.now)
        )

    /**
     * The title lives in the shared chrome now, not in the stateless body, so this
     * renders the same pairing the screen does rather than dropping the assertion.
     */
    @Test
    fun showsTheTitle() = runComposeUiTest {
        setContent {
            DivaTheme {
                DivaScaffold(title = "Activity") { ActivityContent(ActivityUiState()) }
            }
        }
        onNodeWithText("Activity").assertIsDisplayed()
    }

    @Test
    fun showsAnEmptyStateWithNothingToShow() = runComposeUiTest {
        setContent { DivaTheme { ActivityContent(ActivityUiState()) } }
        onNodeWithText("Nothing here yet").assertIsDisplayed()
    }

    /** A filtered empty list is a different situation and must not read as "no data". */
    @Test
    fun distinguishesAnEmptyFilterResult() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(
                    ActivityUiState(filter = ActivityFilterState(query = "zzz")),
                )
            }
        }
        onNodeWithText("Nothing matches those filters").assertIsDisplayed()
    }

    @Test
    fun rendersAllThreeKindsTogether() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(ActivityUiState(items = listOf(spend(), debt(), insight())))
            }
        }

        onNodeWithText("Blue Bottle").assertIsDisplayed()
        onNodeWithText("Sam").assertIsDisplayed()
        onNodeWithText("Quiet week").assertIsDisplayed()
    }

    @Test
    fun labelsWhoOwesWhom() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(
                    ActivityUiState(
                        items = listOf(
                            debt(id = "l1", name = "Sam", kind = LedgerEntryKind.LENT),
                            debt(id = "l2", name = "Alex", kind = LedgerEntryKind.BORROWED),
                        )
                    )
                )
            }
        }

        onNodeWithText("owes you").assertIsDisplayed()
        onNodeWithText("you owe").assertIsDisplayed()
    }

    /** A split shows both the charged amount and the part that was actually yours. */
    @Test
    fun showsYourShareOnASplitSpend() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(ActivityUiState(items = listOf(spend(othersShare = 80.0))))
            }
        }

        onNodeWithText("Your share 40.00").assertIsDisplayed()
        onNodeWithText("-120.00").assertIsDisplayed()
    }

    @Test
    fun doesNotShowAShareLineForAnOrdinarySpend() = runComposeUiTest {
        setContent {
            DivaTheme { ActivityContent(ActivityUiState(items = listOf(spend()))) }
        }

        assertEquals(0, onAllNodesWithText("Your share", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun offersKindAndPeriodFilters() = runComposeUiTest {
        setContent { DivaTheme { ActivityContent(ActivityUiState()) } }

        onNodeWithText("Spending").assertIsDisplayed()
        onNodeWithText("Debts").assertIsDisplayed()
        onNodeWithText("Insights").assertIsDisplayed()
        onNodeWithText("This month").assertIsDisplayed()
    }

    @Test
    fun reportsAKindToggle() = runComposeUiTest {
        var toggled: ActivityKind? = null
        setContent {
            DivaTheme { ActivityContent(ActivityUiState(), onToggleKind = { toggled = it }) }
        }

        onNodeWithText("Debts").performClick()
        assertEquals(ActivityKind.DEBTS, toggled)
    }

    @Test
    fun offersToClearOnlyWhenFiltered() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(ActivityUiState(filter = ActivityFilterState(query = "x")))
            }
        }
        onNodeWithText("Clear").assertIsDisplayed()
    }

    @Test
    fun hidesTheBalancesSummaryWhenEverythingIsSettled() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(
                    ActivityUiState(
                        balances = listOf(
                            PersonBalance(TestData.person(id = "p1", name = "Sam"), balance = 0.0, entryCount = 2)
                        )
                    )
                )
            }
        }

        assertEquals(0, onAllNodesWithText("Owed to you").fetchSemanticsNodes().size)
    }

    @Test
    fun showsOutstandingBalances() = runComposeUiTest {
        setContent {
            DivaTheme {
                ActivityContent(
                    ActivityUiState(
                        balances = listOf(
                            PersonBalance(TestData.person(id = "p1", name = "Sam"), balance = 40.0, entryCount = 1),
                            PersonBalance(TestData.person(id = "p2", name = "Alex"), balance = -15.0, entryCount = 1),
                        )
                    )
                )
            }
        }

        onNodeWithText("Owed to you").assertIsDisplayed()
        onNodeWithText("40.00").assertIsDisplayed()
        onNodeWithText("You owe").assertIsDisplayed()
        onNodeWithText("15.00").assertIsDisplayed()
    }
}
