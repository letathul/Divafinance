package com.divafinance.app

import com.divafinance.core.domain.usecase.reports.ReportPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DivaRoutesTest {

    @Test
    fun shellRoutesAreDefined() {
        assertEquals("onboarding", DivaRoutes.ONBOARDING)
        assertEquals("feed", DivaRoutes.FEED)
        assertEquals("you", DivaRoutes.YOU)
        assertEquals("add", DivaRoutes.ADD_EXPENSE)
    }

    @Test
    fun detailRoutesAreDefined() {
        assertEquals("report/{period}/{anchor}", DivaRoutes.REPORT)
        assertEquals("transactions", DivaRoutes.TRANSACTIONS)
        assertEquals("transactions/detail/{transactionId}", DivaRoutes.TRANSACTION_DETAIL)
        assertEquals("budgets", DivaRoutes.BUDGETS)
        assertEquals("cards", DivaRoutes.CARDS)
        assertEquals("cards/{cardId}", DivaRoutes.CARD_DETAIL)
        assertEquals("cards/add", DivaRoutes.CARD_ADD)
        assertEquals("cards/edit", DivaRoutes.CARD_EDIT)
        assertEquals("cards/best", DivaRoutes.BEST_CARD)
        assertEquals("cards/rewards", DivaRoutes.REWARD_MAPPER)
        assertEquals("people", DivaRoutes.PEOPLE)
        assertEquals("people/{personId}", DivaRoutes.PERSON_DETAIL)
        assertEquals("graphs", DivaRoutes.GRAPHS)
        assertEquals("graphs/thresholds", DivaRoutes.THRESHOLD_CONFIG)
        assertEquals("map", DivaRoutes.MAP)
        assertEquals("scanner", DivaRoutes.SCANNER)
        assertEquals("scanner/review/{receiptId}", DivaRoutes.RECEIPT_REVIEW)
        assertEquals("backup", DivaRoutes.BACKUP)
        assertEquals("automation", DivaRoutes.AUTOMATION)
        assertEquals("settings", DivaRoutes.SETTINGS)
    }

    @Test
    fun cardDetailRouteFormatting() {
        assertEquals("cards/abc123", DivaRoutes.cardDetail("abc123"))
        assertEquals("cards/my-card", DivaRoutes.cardDetail("my-card"))
    }

    @Test
    fun personDetailRouteFormatting() {
        assertEquals("people/p1", DivaRoutes.personDetail("p1"))
    }

    @Test
    fun transactionDetailRouteFormatting() {
        assertEquals("transactions/detail/t1", DivaRoutes.transactionDetail("t1"))
    }

    @Test
    fun receiptReviewRouteMatchesItsPattern() {
        val built = DivaRoutes.receiptReview("r1")
        assertEquals("scanner/review/r1", built)
        assertEquals(
            DivaRoutes.RECEIPT_REVIEW.split("/").size,
            built.split("/").size,
        )
    }

    /**
     * The built path has to line up with the pattern the graph registers, or the
     * destination is simply never found at runtime.
     */
    @Test
    fun reportRouteMatchesItsPattern() {
        val built = DivaRoutes.report(ReportPeriod.MONTH, LocalDate(2026, 8, 17))
        assertEquals("report/month/2026-08-17", built)

        val patternSegments = DivaRoutes.REPORT.split("/")
        val builtSegments = built.split("/")
        assertEquals(patternSegments.size, builtSegments.size)
        assertEquals(patternSegments[0], builtSegments[0])
    }

    @Test
    fun reportPeriodParsesBackFromTheRouteSegment() {
        ReportPeriod.entries.forEach { period ->
            val segment = DivaRoutes.report(period, LocalDate(2026, 1, 1)).split("/")[1]
            assertEquals(period, ReportPeriod.fromName(segment))
        }
    }

    /** Only the two tabs, and neither is a detail page. */
    @Test
    fun tabsAreDistinctTopLevelRoutes() {
        assertTrue(DivaRoutes.FEED != DivaRoutes.YOU)
        assertTrue(!DivaRoutes.FEED.contains("/"))
        assertTrue(!DivaRoutes.YOU.contains("/"))
    }

    @Test
    fun theAutomationUriPrefixMatchesWhatAutomationViewModelMints() {
        // Stated in :feature:automation too, which sits below this module and can't import
        // DivaRoutes. This is the assertion that keeps the two halves in step.
        assertEquals("divafinance://automation/", DivaRoutes.AUTOMATION_URI_PREFIX)
    }

    @Test
    fun automationDeepLinksResolveToDestinations() {
        assertEquals(
            DivaRoutes.ADD_EXPENSE,
            DivaRoutes.forAutomationDeepLink("divafinance://automation/quick_expense"),
        )
        assertEquals(
            DivaRoutes.FEED,
            DivaRoutes.forAutomationDeepLink("divafinance://automation/daily_summary"),
        )
        assertEquals(
            DivaRoutes.BUDGETS,
            DivaRoutes.forAutomationDeepLink("divafinance://automation/budget_alert"),
        )
    }

    /** A stale shortcut from an older install must do nothing, not crash or misroute. */
    @Test
    fun anUnknownAutomationDeepLinkResolvesToNothing() {
        assertNull(DivaRoutes.forAutomationDeepLink("divafinance://automation/gone"))
        assertNull(DivaRoutes.forAutomationDeepLink("divafinance://automation/"))
    }

    @Test
    fun aNonAutomationUriResolvesToNothing() {
        assertNull(DivaRoutes.forAutomationDeepLink("https://example.com/quick_expense"))
        assertNull(DivaRoutes.forAutomationDeepLink("quick_expense"))
    }
}
