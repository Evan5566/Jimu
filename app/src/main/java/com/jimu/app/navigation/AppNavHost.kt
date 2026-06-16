package com.jimu.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.jimu.app.ui.components.JimuBottomBar
import com.jimu.app.ui.goals.GoalsScreen
import com.jimu.app.ui.habits.HabitsScreen
import com.jimu.app.ui.home.HomeScreen
import com.jimu.app.ui.review.ReviewHistoryScreen
import com.jimu.app.ui.review.ReviewScreen
import com.jimu.app.ui.tasks.TasksScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val tabs = listOf(
        Routes.Home,
        Routes.Tasks,
        Routes.Habits,
        Routes.Goals,
        Routes.Review
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    val tabRoutes = remember(tabs) { tabs.map { route -> route.route }.toSet() }
    var homeResetScrollSignal by remember { mutableIntStateOf(0) }

    fun navigateToTab(route: String) {
        val currentRoute = currentDestination?.route
        val shouldResetHomeScroll = shouldResetHomeScrollOnTabClick(
            currentRoute = currentRoute,
            targetRoute = route,
            tabRoutes = tabRoutes
        )
        val shouldRestoreState = shouldRestoreTabState(
            currentRoute = currentRoute,
            targetRoute = route,
            tabRoutes = tabRoutes
        )
        val shouldSaveState = shouldSaveTabState(
            currentRoute = currentRoute,
            targetRoute = route,
            tabRoutes = tabRoutes
        )

        if (shouldResetHomeScroll) {
            homeResetScrollSignal += 1
        }

        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = shouldSaveState
            }
            launchSingleTop = true
            restoreState = shouldRestoreState
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            JimuBottomBar(
                tabs = tabs,
                currentDestinationRoute = currentDestination?.route,
                onTabClick = { route -> navigateToTab(route) }
            )
        }
    ) { innerPadding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.Home.route) {
                HomeScreen(
                    innerPadding = innerPadding,
                    resetScrollSignal = homeResetScrollSignal,
                    onOpenReview = {
                        navigateToTab(Routes.Review.route)
                    }
                )
            }
            composable(Routes.Tasks.route) {
                TasksScreen(innerPadding)
            }
            composable(Routes.Habits.route) {
                HabitsScreen(innerPadding)
            }
            composable(Routes.Goals.route) {
                GoalsScreen(innerPadding)
            }
            composable(Routes.Review.route) {
                ReviewScreen(
                    innerPadding = innerPadding,
                    isTopLevelTab = true,
                    onOpenHistory = {
                        navController.navigate(Routes.ReviewHistory.route) {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.ReviewHistory.route) {
                ReviewHistoryScreen(
                    innerPadding = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenReview = { reviewDate ->
                        navController.navigate(Routes.ReviewByDate.createRoute(reviewDate)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = Routes.ReviewByDate.route,
                arguments = listOf(
                    navArgument(Routes.ReviewByDate.ARG_REVIEW_DATE) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val reviewDate = backStackEntry.arguments
                    ?.getString(Routes.ReviewByDate.ARG_REVIEW_DATE)
                    .orEmpty()

                ReviewScreen(
                    innerPadding = innerPadding,
                    reviewDate = reviewDate,
                    onOpenHistory = {
                        navController.navigate(Routes.ReviewHistory.route) {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
