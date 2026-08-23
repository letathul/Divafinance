package com.divafinance.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read
import com.divafinance.core.domain.usecase.reports.ReportPeriod
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.feature.activity.ActivityScreen
import com.divafinance.feature.activity.ActivityViewModel
import com.divafinance.feature.activity.PersonDetailScreen
import com.divafinance.feature.activity.PersonDetailViewModel
import com.divafinance.feature.automation.AutomationScreen
import com.divafinance.feature.backup.BackupRestoreScreen
import com.divafinance.feature.backup.BackupViewModel
import com.divafinance.feature.cards.AddEditCardScreen
import com.divafinance.feature.cards.BestCardRecommendationScreen
import com.divafinance.feature.cards.CardDetailScreen
import com.divafinance.feature.cards.CardsListScreen
import com.divafinance.feature.cards.CardsViewModel
import com.divafinance.feature.cards.RewardMapperScreen
import com.divafinance.feature.feed.FeedScreen
import com.divafinance.feature.feed.FeedViewModel
import com.divafinance.feature.graphs.GraphsDashboardScreen
import com.divafinance.feature.graphs.GraphsViewModel
import com.divafinance.feature.graphs.ThresholdConfigScreen
import com.divafinance.feature.map.MapViewModel
import com.divafinance.feature.map.SpendingMapScreen
import com.divafinance.feature.onboarding.OnboardingScreen
import com.divafinance.feature.quickadd.AddExpenseScreen
import com.divafinance.feature.quickadd.QuickAddViewModel
import com.divafinance.feature.scanner.ScannerScreen
import com.divafinance.feature.scanner.ScannerViewModel
import com.divafinance.feature.scanner.review.ReceiptReviewScreen
import com.divafinance.feature.scanner.review.ReceiptReviewViewModel
import com.divafinance.feature.settings.SettingsScreen
import com.divafinance.feature.settings.SettingsViewModel
import com.divafinance.feature.settings.YouScreen
import com.divafinance.feature.settings.YouViewModel
import com.divafinance.feature.transactions.ReportScreen
import com.divafinance.feature.transactions.ReportViewModel
import com.divafinance.feature.transactions.TransactionDetailScreen
import com.divafinance.feature.transactions.TransactionListScreen
import com.divafinance.feature.transactions.TransactionsViewModel
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Every route in one place.
 *
 * Three of these are the shell — [FEED], [YOU] and [ADD_EXPENSE]; everything else is a
 * detail page reached from one of them. `DivaRoutesTest` asserts each literal, so a
 * rename here is a two-file change by design.
 */
object DivaRoutes {
    const val ONBOARDING = "onboarding"

    // ── the shell ──
    const val FEED = "feed"
    const val YOU = "you"
    const val ADD_EXPENSE = "add"

    // ── from the feed ──
    const val REPORT = "report/{period}/{anchor}"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transactions/detail/{transactionId}"

    // ── from You ──
    const val BUDGETS = "budgets"
    const val CARDS = "cards"
    const val CARD_DETAIL = "cards/{cardId}"
    const val CARD_ADD = "cards/add"
    const val CARD_EDIT = "cards/edit"
    const val BEST_CARD = "cards/best"
    const val REWARD_MAPPER = "cards/rewards"
    const val PEOPLE = "people"
    const val PERSON_DETAIL = "people/{personId}"
    const val GRAPHS = "graphs"
    const val THRESHOLD_CONFIG = "graphs/thresholds"
    const val MAP = "map"
    const val SCANNER = "scanner"
    const val RECEIPT_REVIEW = "scanner/review/{receiptId}"
    const val BACKUP = "backup"
    const val AUTOMATION = "automation"
    const val SETTINGS = "settings"

    fun cardDetail(cardId: String) = "cards/$cardId"

    fun personDetail(personId: String) = "people/$personId"

    fun transactionDetail(transactionId: String) = "transactions/detail/$transactionId"

    fun receiptReview(receiptId: String) = "scanner/review/$receiptId"

    /**
     * A period plus any date inside it. Two segments rather than a start and an end,
     * because the period already knows how to derive its own bounds from the anchor.
     */
    fun report(period: ReportPeriod, anchor: LocalDate) =
        "report/${period.name.lowercase()}/$anchor"

    /**
     * Where `divafinance://automation/{id}` lands, or null for anything unrecognised — a
     * stale launcher shortcut from a previous install must do nothing rather than crash.
     *
     * The URI format is minted in `AutomationViewModel` and matched here. It is stated
     * twice because `:feature:automation` sits below this module and cannot import
     * [DivaRoutes]; `DivaRoutesTest` pins this side of it.
     */
    fun forAutomationDeepLink(uri: String): String? {
        val id = uri.removePrefix(AUTOMATION_URI_PREFIX).takeIf { it != uri } ?: return null
        return when (id) {
            "quick_expense", "android_nfc", "ios_siri", "ios_shortcuts" -> ADD_EXPENSE
            "daily_summary", "android_widget" -> FEED
            "budget_alert" -> BUDGETS
            else -> null
        }
    }

    const val AUTOMATION_URI_PREFIX = "divafinance://automation/"
}

@Composable
fun DivaNavHost(
    navController: NavHostController,
    startDestination: String,
    quickAddViewModel: QuickAddViewModel,
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
                    navController.navigate(DivaRoutes.FEED) {
                        popUpTo(DivaRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        // ── Feed ──────────────────────────────────────────────────────────────
        composable(DivaRoutes.FEED) {
            val feedViewModel: FeedViewModel = koinViewModel()
            FeedScreen(
                viewModel = feedViewModel,
                onOpenReport = { period, anchor ->
                    navController.navigate(DivaRoutes.report(period, anchor))
                },
                onOpenTransaction = { id ->
                    navController.navigate(DivaRoutes.transactionDetail(id))
                },
                onOpenSearch = { navController.navigate(DivaRoutes.TRANSACTIONS) },
                onOpenInsights = { navController.navigate(DivaRoutes.GRAPHS) },
                onOpenProfile = { navController.navigate(DivaRoutes.YOU) },
            )
        }

        composable(DivaRoutes.REPORT) { backStackEntry ->
            // `arguments` is a multiplatform SavedState, not an Android Bundle, so it is
            // read through the savedstate reader rather than getString().
            val args = backStackEntry.arguments
            val period = ReportPeriod.fromName(args?.read { getStringOrNull("period") })
            val anchor = args?.read { getStringOrNull("anchor") }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: return@composable
            val reportViewModel: ReportViewModel =
                koinViewModel(key = "$period-$anchor") { parametersOf(period, anchor) }
            ReportScreen(
                viewModel = reportViewModel,
                onBack = { navController.popBackStack() },
                onOpenTransaction = { id ->
                    navController.navigate(DivaRoutes.transactionDetail(id))
                },
            )
        }

        composable(DivaRoutes.TRANSACTIONS) {
            TransactionListScreen(
                onBack = { navController.popBackStack() },
                viewModel = transactionsViewModel,
            )
        }

        composable(DivaRoutes.TRANSACTION_DETAIL) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.read { getStringOrNull("transactionId") }
                ?: return@composable
            // The unfiltered flow on purpose: `filteredTransactions` hides rows the list
            // screen's category/type/search filter excludes, and the feed shares this
            // hoisted ViewModel — so a filter set there used to blank this screen.
            val transactions by transactionsViewModel.transactions.collectAsState()
            val transaction = transactions.firstOrNull { it.id == transactionId }
            if (transaction == null) {
                // The flow starts at `emptyList()`, so a miss on first composition means
                // "not loaded yet", not "no such transaction".
                LoadingIndicator(Modifier.fillMaxSize())
            } else {
                TransactionDetailScreen(
                    transaction = transaction,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // ── Add ───────────────────────────────────────────────────────────────
        composable(DivaRoutes.ADD_EXPENSE) {
            val saved by quickAddViewModel.saved.collectAsState()
            // A save returns to the feed; the undo snackbar is hosted by the shell, so it
            // survives this screen going away. `saved` is only cleared once that snackbar
            // resolves, so it can still be set when the screen is reopened — hence
            // comparing against the value seen on entry rather than just null-checking.
            val savedOnEntry = remember { saved?.transactionId }
            LaunchedEffect(saved) {
                val id = saved?.transactionId
                if (id != null && id != savedOnEntry) navController.popBackStack()
            }
            AddExpenseScreen(
                onDismiss = { navController.popBackStack() },
                viewModel = quickAddViewModel,
                onOpenScanner = { navController.navigate(DivaRoutes.SCANNER) },
            )
        }

        // ── You ───────────────────────────────────────────────────────────────
        composable(DivaRoutes.YOU) {
            val youViewModel: YouViewModel = koinViewModel()
            YouScreen(
                viewModel = youViewModel,
                onLogExpense = { navController.navigate(DivaRoutes.ADD_EXPENSE) },
                onOpenBudgets = { navController.navigate(DivaRoutes.BUDGETS) },
                onOpenCards = { navController.navigate(DivaRoutes.CARDS) },
                onOpenGraphs = { navController.navigate(DivaRoutes.GRAPHS) },
                onOpenPeople = { navController.navigate(DivaRoutes.PEOPLE) },
                onOpenMap = { navController.navigate(DivaRoutes.MAP) },
                onOpenScanner = { navController.navigate(DivaRoutes.SCANNER) },
                onOpenBackup = { navController.navigate(DivaRoutes.BACKUP) },
                onOpenAutomation = { navController.navigate(DivaRoutes.AUTOMATION) },
                onOpenSettings = { navController.navigate(DivaRoutes.SETTINGS) },
            )
        }

        composable(DivaRoutes.BUDGETS) {
            ThresholdConfigScreen(
                onBack = { navController.popBackStack() },
                viewModel = graphsViewModel,
            )
        }

        composable(DivaRoutes.CARDS) {
            CardsListScreen(
                onAddCard = { navController.navigate(DivaRoutes.CARD_ADD) },
                onCardClick = { cardId -> navController.navigate(DivaRoutes.cardDetail(cardId)) },
                onBack = { navController.popBackStack() },
                onBestCard = { navController.navigate(DivaRoutes.BEST_CARD) },
                onRewardMapper = { navController.navigate(DivaRoutes.REWARD_MAPPER) },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.CARD_DETAIL) { backStackEntry ->
            val cardId = backStackEntry.arguments?.read { getStringOrNull("cardId") }
                ?: return@composable
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

        composable(DivaRoutes.REWARD_MAPPER) {
            RewardMapperScreen(
                onBack = { navController.popBackStack() },
                viewModel = cardsViewModel,
            )
        }

        composable(DivaRoutes.PEOPLE) {
            val activityViewModel: ActivityViewModel = koinViewModel()
            ActivityScreen(
                onPersonClick = { personId ->
                    navController.navigate(DivaRoutes.personDetail(personId))
                },
                onBack = { navController.popBackStack() },
                viewModel = activityViewModel,
            )
        }

        composable(DivaRoutes.PERSON_DETAIL) { backStackEntry ->
            val personId = backStackEntry.arguments?.read { getStringOrNull("personId") }
                ?: return@composable
            val detailViewModel: PersonDetailViewModel =
                koinViewModel(key = personId) { parametersOf(personId) }
            PersonDetailScreen(
                onBack = { navController.popBackStack() },
                viewModel = detailViewModel,
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
                onReviewReceipt = { id -> navController.navigate(DivaRoutes.receiptReview(id)) },
                onOpenTransaction = { id ->
                    navController.navigate(DivaRoutes.transactionDetail(id))
                },
                viewModel = scannerViewModel,
            )
        }

        composable(DivaRoutes.RECEIPT_REVIEW) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.read { getStringOrNull("receiptId") }
                ?: return@composable
            val reviewViewModel: ReceiptReviewViewModel =
                koinViewModel(key = receiptId) { parametersOf(receiptId) }
            ReceiptReviewScreen(
                onBack = { navController.popBackStack() },
                // Saving returns to where the scan started rather than leaving a stale
                // review form on the back stack.
                onSaved = { navController.popBackStack(DivaRoutes.SCANNER, inclusive = false) },
                viewModel = reviewViewModel,
            )
        }

        composable(DivaRoutes.BACKUP) {
            val backupViewModel: BackupViewModel = koinViewModel()
            BackupRestoreScreen(
                onBack = { navController.popBackStack() },
                viewModel = backupViewModel,
            )
        }

        composable(DivaRoutes.AUTOMATION) {
            AutomationScreen(onBack = { navController.popBackStack() })
        }

        composable(DivaRoutes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBackup = { navController.navigate(DivaRoutes.BACKUP) },
                onNavigateToScanner = { navController.navigate(DivaRoutes.SCANNER) },
                onNavigateToAutomation = { navController.navigate(DivaRoutes.AUTOMATION) },
                isServerRunning = settingsState.isServerRunning,
                serverPort = settingsState.serverPort,
                serverUrl = settingsState.serverUrl,
                serverError = settingsState.serverError,
                onToggleServer = settingsViewModel::toggleServer,
                isDemoActive = settingsState.isDemoActive,
                isRemovingDemo = settingsState.isRemovingDemo,
                onRemoveDemo = settingsViewModel::removeDemoData,
                themeMode = settingsState.themeMode,
                accent = settingsState.accent,
                onThemeModeChange = settingsViewModel::setThemeMode,
                onAccentChange = settingsViewModel::setAccent,
                locationCaptureMode = settingsState.locationCaptureMode,
                onLocationCaptureModeChange = settingsViewModel::setLocationCaptureMode,
                isSmartReadingSupported = settingsState.isSmartReadingSupported,
                isSmartReadingEnabled = settingsState.isSmartReadingEnabled,
                isSmartReadingDownloading = settingsState.isSmartReadingDownloading,
                onSmartReadingChange = settingsViewModel::setSmartReading,
            )
        }
    }
}
