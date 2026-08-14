package com.divafinance.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.divafinance.feature.automation.AutomationScreen
import com.divafinance.feature.backup.BackupRestoreScreen
import com.divafinance.feature.backup.BackupViewModel
import com.divafinance.feature.cards.AddEditCardScreen
import com.divafinance.feature.cards.BestCardRecommendationScreen
import com.divafinance.feature.cards.CardDetailScreen
import com.divafinance.feature.cards.CardsListScreen
import com.divafinance.feature.cards.CardsViewModel
import com.divafinance.feature.dashboard.DashboardScreen
import com.divafinance.feature.feed.FeedScreen
import com.divafinance.feature.feed.FeedViewModel
import com.divafinance.feature.graphs.GraphsDashboardScreen
import com.divafinance.feature.graphs.GraphsViewModel
import com.divafinance.feature.graphs.ThresholdConfigScreen
import com.divafinance.feature.map.MapViewModel
import com.divafinance.feature.map.SpendingMapScreen
import com.divafinance.server.DivaServer
import com.divafinance.feature.onboarding.OnboardingScreen
import com.divafinance.feature.scanner.ScannerScreen
import com.divafinance.feature.scanner.ScannerViewModel
import com.divafinance.feature.settings.SettingsScreen
import com.divafinance.feature.transactions.AddTransactionScreen
import com.divafinance.feature.transactions.TransactionListScreen
import com.divafinance.feature.transactions.TransactionsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

object DivaRoutes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val CARDS = "cards"
    const val CARD_DETAIL = "cards/{cardId}"
    const val CARD_ADD = "cards/add"
    const val CARD_EDIT = "cards/edit"
    const val BEST_CARD = "cards/best"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_ADD = "transactions/add"
    const val FEED = "feed"
    const val SETTINGS = "settings"
    const val GRAPHS = "graphs"
    const val MAP = "map"
    const val SCANNER = "scanner"
    const val BACKUP = "backup"
    const val THRESHOLD_CONFIG = "graphs/thresholds"
    const val AUTOMATION = "automation"

    fun cardDetail(cardId: String) = "cards/$cardId"
}

@Composable
fun DivaNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    val cardsViewModel: CardsViewModel = koinViewModel()
    val transactionsViewModel: TransactionsViewModel = koinViewModel()
    val graphsViewModel: GraphsViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(DivaRoutes.ONBOARDING) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(DivaRoutes.DASHBOARD) {
                        popUpTo(DivaRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(DivaRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToCards = {
                    navController.navigate(DivaRoutes.CARDS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTransactions = {
                    navController.navigate(DivaRoutes.TRANSACTIONS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBestCard = {
                    navController.navigate(DivaRoutes.BEST_CARD)
                },
                onNavigateToGraphs = {
                    navController.navigate(DivaRoutes.GRAPHS)
                },
                onNavigateToMap = {
                    navController.navigate(DivaRoutes.MAP)
                },
            )
        }

        composable(DivaRoutes.CARDS) {
            CardsListScreen(
                onAddCard = { navController.navigate(DivaRoutes.CARD_ADD) },
                onCardClick = { cardId -> navController.navigate(DivaRoutes.cardDetail(cardId)) },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.CARD_DETAIL) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() },
                onEdit = { card ->
                    cardsViewModel.loadCardForEditing(card)
                    navController.navigate(DivaRoutes.CARD_EDIT)
                },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.CARD_ADD) {
            AddEditCardScreen(
                onBack = { navController.popBackStack() },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.CARD_EDIT) {
            AddEditCardScreen(
                onBack = { navController.popBackStack() },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.BEST_CARD) {
            BestCardRecommendationScreen(
                onBack = { navController.popBackStack() },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.TRANSACTIONS) {
            TransactionListScreen(
                onAddTransaction = {
                    navController.navigate(DivaRoutes.TRANSACTION_ADD)
                },
                viewModel = transactionsViewModel,
            )
        }

        composable(DivaRoutes.TRANSACTION_ADD) {
            AddTransactionScreen(
                onBack = { navController.popBackStack() },
                viewModel = transactionsViewModel,
            )
        }

        composable(DivaRoutes.FEED) {
            val feedViewModel: FeedViewModel = koinViewModel()
            FeedScreen(viewModel = feedViewModel)
        }
        composable(DivaRoutes.SETTINGS) {
            val divaServer = koinInject<DivaServer>()
            SettingsScreen(
                onNavigateToBackup = {
                    navController.navigate(DivaRoutes.BACKUP)
                },
                isServerRunning = divaServer.isRunning(),
                onToggleServer = { enabled ->
                    if (enabled) divaServer.start() else divaServer.stop()
                },
            )
        }
        composable(DivaRoutes.GRAPHS) {
            GraphsDashboardScreen(
                onNavigateToThresholdConfig = {
                    navController.navigate(DivaRoutes.THRESHOLD_CONFIG)
                },
                viewModel = graphsViewModel,
            )
        }

        composable(DivaRoutes.THRESHOLD_CONFIG) {
            ThresholdConfigScreen(
                onBack = { navController.popBackStack() },
                viewModel = graphsViewModel,
            )
        }
        composable(DivaRoutes.MAP) {
            val mapViewModel: MapViewModel = koinViewModel()
            SpendingMapScreen(
                onBack = { navController.popBackStack() },
                viewModel = mapViewModel,
            )
        }
        composable(DivaRoutes.SCANNER) {
            val scannerViewModel: ScannerViewModel = koinViewModel()
            ScannerScreen(
                onBack = { navController.popBackStack() },
                viewModel = scannerViewModel,
            )
        }
        composable(DivaRoutes.BACKUP) {
            val backupViewModel: BackupViewModel = koinViewModel()
            BackupRestoreScreen(
                onBack = { navController.popBackStack() },
                viewModel = backupViewModel,
            )
        }
        composable(DivaRoutes.AUTOMATION) { AutomationScreen() }
    }
}
