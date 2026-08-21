package com.divafinance.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.onboarding.InitializeDatabaseUseCase
import com.divafinance.core.ui.component.DivaTab
import com.divafinance.core.ui.component.FloatingTabBar
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.feature.quickadd.QuickAddViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Routes the tab capsule stays visible over.
 *
 * `currentBackStackEntry` reports the route *pattern* ("cards/{cardId}"), never the
 * resolved path, so membership is tested against the patterns in [DivaRoutes] rather
 * than against a navigated URL.
 */
private val tabRoutes = setOf(DivaRoutes.FEED, DivaRoutes.YOU)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val initializeDatabase = koinInject<InitializeDatabaseUseCase>()

    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf(DivaRoutes.ONBOARDING) }

    LaunchedEffect(Unit) {
        val onboardingCompleted = initializeDatabase()
        startDestination = if (onboardingCompleted) DivaRoutes.FEED else DivaRoutes.ONBOARDING
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    var currentRoute by remember { mutableStateOf(startDestination) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            currentRoute = entry.destination.route ?: DivaRoutes.FEED
        }
    }

    // The sheet's ViewModel is hoisted here, not resolved inside the add destination, so
    // the undo snackbar outlives the screen that produced it.
    val quickAddViewModel: QuickAddViewModel = koinViewModel()
    val saved by quickAddViewModel.saved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        val justSaved = saved ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Added ${justSaved.amount.toFixed(2)} · ${justSaved.category.displayName}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            quickAddViewModel.undo()
        } else {
            quickAddViewModel.consumeSaved()
        }
    }

    // No Scaffold: the capsule floats *over* the content rather than displacing it, so
    // the ledger keeps scrolling behind it. Screens leave room via their contentPadding.
    Box(Modifier.fillMaxSize()) {
        DivaNavHost(
            navController = navController,
            startDestination = startDestination,
            quickAddViewModel = quickAddViewModel,
        )

        if (currentRoute in tabRoutes) {
            FloatingTabBar(
                selected = if (currentRoute == DivaRoutes.YOU) DivaTab.YOU else DivaTab.FEED,
                onSelect = { tab ->
                    val route = if (tab == DivaTab.YOU) DivaRoutes.YOU else DivaRoutes.FEED
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(DivaRoutes.FEED) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onCompose = { navController.navigate(DivaRoutes.ADD_EXPENSE) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 104.dp),
        )
    }
}
