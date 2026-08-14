package com.divafinance.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.divafinance.feature.automation.AutomationScreen
import com.divafinance.feature.backup.BackupRestoreScreen
import com.divafinance.feature.cards.CardsListScreen
import com.divafinance.feature.dashboard.DashboardScreen
import com.divafinance.feature.feed.FeedScreen
import com.divafinance.feature.graphs.GraphsDashboardScreen
import com.divafinance.feature.map.SpendingMapScreen
import com.divafinance.feature.onboarding.OnboardingScreen
import com.divafinance.feature.scanner.ScannerScreen
import com.divafinance.feature.settings.SettingsScreen
import com.divafinance.feature.transactions.TransactionListScreen

object DivaRoutes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val CARDS = "cards"
    const val TRANSACTIONS = "transactions"
    const val FEED = "feed"
    const val SETTINGS = "settings"
    const val GRAPHS = "graphs"
    const val MAP = "map"
    const val SCANNER = "scanner"
    const val BACKUP = "backup"
    const val AUTOMATION = "automation"
}

@Composable
fun DivaNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
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

        composable(DivaRoutes.DASHBOARD) { DashboardScreen() }
        composable(DivaRoutes.CARDS) { CardsListScreen() }
        composable(DivaRoutes.TRANSACTIONS) { TransactionListScreen() }
        composable(DivaRoutes.FEED) { FeedScreen() }
        composable(DivaRoutes.SETTINGS) { SettingsScreen() }
        composable(DivaRoutes.GRAPHS) { GraphsDashboardScreen() }
        composable(DivaRoutes.MAP) { SpendingMapScreen() }
        composable(DivaRoutes.SCANNER) { ScannerScreen() }
        composable(DivaRoutes.BACKUP) { BackupRestoreScreen() }
        composable(DivaRoutes.AUTOMATION) { AutomationScreen() }
    }
}
