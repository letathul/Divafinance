package com.divafinance.feature.cards

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.divafinance.core.domain.usecase.cards.AddCardUseCase
import com.divafinance.core.domain.usecase.cards.GetAllCardsUseCase
import com.divafinance.core.domain.usecase.cards.GetBestCardForCategoryUseCase
import com.divafinance.core.domain.usecase.cards.UpdateCardUseCase
import com.divafinance.core.testing.installTestMainDispatcher
import com.divafinance.core.testing.resetTestMainDispatcher
import com.divafinance.core.testing.fake.FakeCardRepository
import com.divafinance.core.testing.fake.FakeRewardRepository
import com.divafinance.core.testing.fake.FakeSettingsRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.ui.theme.DivaTheme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CardsListScreenTest {

    @BeforeTest
    fun setUpMainDispatcher() = installTestMainDispatcher()

    @AfterTest
    fun tearDownMainDispatcher() = resetTestMainDispatcher()

    private val cardRepo = FakeCardRepository()
    private val rewardRepo = FakeRewardRepository()
    private val settingsRepo = FakeSettingsRepository()

    private fun viewModel() = CardsViewModel(
        GetAllCardsUseCase(cardRepo),
        AddCardUseCase(cardRepo, rewardRepo),
        UpdateCardUseCase(cardRepo, rewardRepo),
        GetBestCardForCategoryUseCase(cardRepo, rewardRepo, settingsRepo),
    )

    /** The screen has no app bar — an empty card list shows this instead. */
    @Test
    fun showsEmptyStateWhenThereAreNoCards() = runComposeUiTest {
        setContent {
            DivaTheme { CardsListScreen(viewModel = viewModel()) }
        }
        onNodeWithText("No Cards Yet").assertIsDisplayed()
    }

    @Test
    fun rendersAStoredCard() = runComposeUiTest {
        cardRepo.setCards(listOf(TestData.card(id = "c1", name = "Sapphire")))

        setContent {
            DivaTheme { CardsListScreen(viewModel = viewModel()) }
        }

        // The name renders twice — once in the carousel, once in the list below it.
        waitUntil {
            onAllNodesWithText("Sapphire", substring = true)
                .fetchSemanticsNodes().size == 2
        }
        onAllNodesWithText("Sapphire", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun hidesEmptyStateOnceACardExists() = runComposeUiTest {
        cardRepo.setCards(listOf(TestData.card(id = "c1", name = "Sapphire")))

        setContent {
            DivaTheme { CardsListScreen(viewModel = viewModel()) }
        }

        waitUntil {
            onAllNodesWithText("No Cards Yet").fetchSemanticsNodes().isEmpty()
        }
    }
}
