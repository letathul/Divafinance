package com.divafinance.app

import androidx.compose.foundation.background
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
import androidx.navigation.compose.rememberNavController
import com.divafinance.core.common.toFixed
import com.divafinance.core.domain.usecase.onboarding.InitializeDatabaseUseCase
import com.divafinance.core.ui.adaptive.DivaBottomBar
import com.divafinance.core.ui.adaptive.DivaTab
import com.divafinance.core.ui.adaptive.DivaTabBar
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.core.ui.theme.Space
import com.divafinance.core.ui.theme.diva
import com.divafinance.feature.quickadd.QuickAddViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Routes the tab bar stays visible over.
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
        // Painted, not transparent: an unpainted first frame flashes the M3 default
        // between the launch screen and the first real one.
        Box(
            Modifier.fillMaxSize().background(diva.canvas),
            contentAlignment = Alignment.Center,
        ) {
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

    // No Scaffold: the bar sits *over* the content rather than displacing it, so the
    // ledger keeps scrolling behind its translucency. Screens leave room for it with
    // `divaContentPadding()` rather than a Scaffold inset.
    Box(Modifier.fillMaxSize()) {
        DivaNavHost(
            navController = navController,
            startDestination = startDestination,
            quickAddViewModel = quickAddViewModel,
        )

        if (currentRoute in tabRoutes) {
            DivaTabBar(
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
                // Edge-pinned: the bar adds the navigation-bar inset itself and draws
                // its fill to the screen edge, so on Android it *is* the nav bar's
                // background.
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = DivaBottomBar.Height + DivaBottomBar.CenterRaise + Space.sm),
        )
    }
}
