package com.divafinance.app

import kotlin.test.Test
import kotlin.test.assertEquals

class DivaRoutesTest {

    @Test
    fun allRoutesAreDefined() {
        assertEquals("onboarding", DivaRoutes.ONBOARDING)
        assertEquals("dashboard", DivaRoutes.DASHBOARD)
        assertEquals("cards", DivaRoutes.CARDS)
        assertEquals("cards/{cardId}", DivaRoutes.CARD_DETAIL)
        assertEquals("cards/add", DivaRoutes.CARD_ADD)
        assertEquals("cards/edit", DivaRoutes.CARD_EDIT)
        assertEquals("cards/best", DivaRoutes.BEST_CARD)
        assertEquals("activity", DivaRoutes.ACTIVITY)
        assertEquals("people/{personId}", DivaRoutes.PERSON_DETAIL)
        assertEquals("transactions", DivaRoutes.TRANSACTIONS)
        assertEquals("transactions/add", DivaRoutes.TRANSACTION_ADD)
        assertEquals("feed", DivaRoutes.FEED)
        assertEquals("settings", DivaRoutes.SETTINGS)
        assertEquals("graphs", DivaRoutes.GRAPHS)
        assertEquals("map", DivaRoutes.MAP)
        assertEquals("scanner", DivaRoutes.SCANNER)
        assertEquals("backup", DivaRoutes.BACKUP)
        assertEquals("graphs/thresholds", DivaRoutes.THRESHOLD_CONFIG)
        assertEquals("automation", DivaRoutes.AUTOMATION)
    }

    @Test
    fun cardDetailRouteFormatting() {
        assertEquals("cards/abc123", DivaRoutes.cardDetail("abc123"))
        assertEquals("cards/my-card", DivaRoutes.cardDetail("my-card"))
    }
}
