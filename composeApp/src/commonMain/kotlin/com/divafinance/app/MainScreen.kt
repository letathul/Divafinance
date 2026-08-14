package com.divafinance.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.rememberNavController
import com.divafinance.core.domain.usecase.onboarding.InitializeDatabaseUseCase
import com.divafinance.core.ui.component.LoadingIndicator
import org.koin.compose.koinInject

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(DivaRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(DivaRoutes.TRANSACTIONS, "Transactions", Icons.Filled.List, Icons.Outlined.List),
    BottomNavItem(DivaRoutes.FEED, "Feed", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem(DivaRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet() + DivaRoutes.CARDS

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

    Scaffold(
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
}
