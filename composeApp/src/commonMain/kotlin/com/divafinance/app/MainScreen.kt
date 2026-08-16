package com.divafinance.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.rememberNavController
import com.divafinance.core.domain.usecase.onboarding.InitializeDatabaseUseCase
import com.divafinance.core.common.toFixed
import com.divafinance.core.ui.component.LoadingIndicator
import com.divafinance.feature.quickadd.QuickAddSheet
import com.divafinance.feature.quickadd.QuickAddViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

// Three tabs, not five. Spending, debts and insights were the same reverse-chronological
// list read three ways, so they merged into Activity rather than each taking a slot — at
// five items the labels no longer fit a 360dp screen.
private val bottomNavItems = listOf(
    BottomNavItem(DivaRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(DivaRoutes.ACTIVITY, "Activity", Icons.Filled.List, Icons.Outlined.List),
    BottomNavItem(DivaRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

// Routes that keep the bar visible without being tabs themselves.
private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet() +
    DivaRoutes.CARDS + DivaRoutes.TRANSACTIONS + DivaRoutes.FEED

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val initializeDatabase = koinInject<InitializeDatabaseUseCase>()

    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf(DivaRoutes.ONBOARDING) }

    LaunchedEffect(Unit) {
        val onboardingCompleted = initializeDatabase()
        startDestination = if (onboardingCompleted) DivaRoutes.DASHBOARD else DivaRoutes.ONBOARDING
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
            currentRoute = entry.destination.route ?: DivaRoutes.DASHBOARD
        }
    }

    val showBottomBar = currentRoute in bottomNavRoutes

    val quickAddViewModel: QuickAddViewModel = koinViewModel()
    var showQuickAdd by remember { mutableStateOf(false) }
    val saved by quickAddViewModel.saved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // The sheet lives here rather than in a feature screen so both Home and Transactions
    // reach it without either depending on the other.
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(onClick = { showQuickAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add transaction")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(DivaRoutes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        DivaNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showQuickAdd) {
        QuickAddSheet(
            onDismiss = { showQuickAdd = false },
            viewModel = quickAddViewModel,
        )
    }
}
