package com.divafinance.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divafinance.core.common.toFixed
import com.divafinance.core.model.CreditCard
import androidx.compose.ui.graphics.vector.ImageVector
import com.divafinance.core.ui.component.CreditCardVisual
import com.divafinance.core.ui.component.DivaCard
import com.divafinance.core.ui.adaptive.DivaScaffold
import com.divafinance.core.ui.component.GlassSurface
import com.divafinance.core.ui.theme.isCupertino
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardsListScreen(
    onAddCard: () -> Unit = {},
    onCardClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onBestCard: () -> Unit = {},
    onRewardMapper: () -> Unit = {},
    viewModel: CardsViewModel = koinViewModel(),
) {
    val cards by viewModel.cards.collectAsState()

    DivaScaffold(
        title = "Cards",
        onBack = onBack,
        // Adding a card is a bar action rather than a FAB: the shell already owns the
        // bottom-right corner, and two floating buttons on one screen collide.
        actions = {
            CardsIconButton(Icons.Outlined.AutoAwesome, "Best card for a purchase", onBestCard)
            CardsIconButton(Icons.Outlined.Tune, "Map reward categories", onRewardMapper)
            CardsIconButton(Icons.Outlined.Add, "Add a card") {
                viewModel.resetForm()
                onAddCard()
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (cards.isEmpty()) {
                EmptyCardsView(modifier = Modifier.align(Alignment.Center))
            } else {
                CardsContent(
                    cards = cards,
                    onCardClick = onCardClick,
                )
            }
        }
    }
}

@Composable
private fun CardsIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    if (isCupertino) {
        // A HIG bar button is a bare tinted glyph, not a chip.
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp).clickable(onClick = onClick),
        )
    } else {
        GlassSurface(Modifier.size(40.dp).clickable(onClick = onClick)) {
            Icon(
                icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center).size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyCardsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No Cards Yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your first credit card to start tracking rewards and getting smart recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardsContent(
    cards: List<CreditCard>,
    onCardClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Your Cards",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        item {
            CardCarousel(cards = cards, onCardClick = onCardClick)
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "All Cards",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        items(cards, key = { it.id }) { card ->
            CardListItem(card = card, onClick = { onCardClick(card.id) })
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun CardCarousel(
    cards: List<CreditCard>,
    onCardClick: (String) -> Unit,
) {
    if (cards.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { cards.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 32.dp),
        pageSpacing = 12.dp,
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

@Composable
private fun CardListItem(
    card: CreditCard,
    onClick: () -> Unit,
) {
    DivaCard(onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${card.network.displayName}${card.lastFour?.let { " •••• $it" } ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.creditLimit > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Limit: ${"$" + card.creditLimit.toFixed(2)} | Available: ${"$" + card.availableCredit.toFixed(2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.rewardRules.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${card.rewardRules.size} reward rule${if (card.rewardRules.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun parseCardColor(hex: String): Color {
    return try {
        Color(("FF" + hex.removePrefix("#")).toLong(16))
    } catch (_: Exception) {
        Color(0xFF1E1E2E)
    }
}
