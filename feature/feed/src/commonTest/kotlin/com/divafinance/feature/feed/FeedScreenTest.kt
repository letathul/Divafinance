package com.divafinance.feature.feed

import com.divafinance.feature.feed.component.formatTimestamp
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue

class FeedScreenTest {

    @Test
    fun formatTimestampShowsTodayForCurrentTime() {
        val now = Clock.System.now()
        val result = formatTimestamp(now)
        assertTrue(result.startsWith("Today at"))
    }
}
