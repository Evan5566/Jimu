package com.jimu.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavigationPolicyTest {

    private val tabRoutes = setOf(
        Routes.Home.route,
        Routes.Tasks.route,
        Routes.Habits.route,
        Routes.Goals.route,
        Routes.Completed.route
    )

    @Test
    fun homeTabFromReviewRouteResetsHomeScrollAndDoesNotRestoreSavedState() {
        assertTrue(
            shouldResetHomeScrollOnTabClick(
                currentRoute = Routes.Review.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )

        assertFalse(
            shouldRestoreTabState(
                currentRoute = Routes.Review.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )
    }

    @Test
    fun homeTabFromAnotherTabKeepsSavedState() {
        assertFalse(
            shouldResetHomeScrollOnTabClick(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )

        assertTrue(
            shouldRestoreTabState(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )
    }
}
