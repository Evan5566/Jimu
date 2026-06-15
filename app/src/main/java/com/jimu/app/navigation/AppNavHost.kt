package com.jimu.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jimu.app.ui.components.JimuBottomBar
import com.jimu.app.ui.completed.CompletedScreen
import com.jimu.app.ui.goals.GoalsScreen
import com.jimu.app.ui.habits.HabitsScreen
import com.jimu.app.ui.home.HomeScreen
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
        Routes.Completed
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            JimuBottomBar(
                tabs = tabs,
                currentDestinationRoute = currentDestination?.route,
                onTabClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
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
                    onOpenReview = {
                        navController.navigate(Routes.Review.route) {
                            launchSingleTop = true
                        }
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
            composable(Routes.Completed.route) {
                CompletedScreen(innerPadding)
            }
            composable(Routes.Review.route) {
                ReviewScreen(
                    innerPadding = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack(
                            route = Routes.Home.route,
                            inclusive = false
                        )
                    }
                )
            }
        }
    }
}
