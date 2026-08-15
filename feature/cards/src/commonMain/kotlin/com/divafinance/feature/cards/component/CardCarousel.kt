package com.divafinance.feature.cards.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divafinance.core.model.CreditCard
import com.divafinance.core.ui.component.CreditCardVisual

@Composable
fun CardCarousel(
    cards: List<CreditCard>,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { cards.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 32.dp),
        pageSpacing = 12.dp,
        modifier = modifier,
    ) { page ->
        val card = cards[page]
        CreditCardVisual(
            name = card.name,
            lastFour = card.lastFour,
            network = card.network.displayName,
            color = parseCardColor(card.color),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun parseCardColor(hex: String): Color {
    return try {
        Color(("FF" + hex.removePrefix("#")).toLong(16))
    } catch (_: Exception) {
        Color(0xFF1E1E2E)
    }
}
