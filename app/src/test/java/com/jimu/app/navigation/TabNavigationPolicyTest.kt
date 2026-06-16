package com.jimu.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavigationPolicyTest {

    private val tabRoutes = setOf(
        Routes.Home.route,
        Routes.Tasks.route,
        Routes.Habits.route,
        Routes.Goals.route,
        Routes.Review.route
    )

    @Test
    fun reviewRouteKeepsPageTitleSeparateFromBottomTabTitle() {
        assertEquals("今日复盘", Routes.Review.title)
        assertEquals("复盘", Routes.Review.tabTitle)
    }

    @Test
    fun homeTabFromReviewHistoryRouteResetsHomeScrollAndDoesNotRestoreSavedState() {
        assertTrue(
            shouldResetHomeScrollOnTabClick(
                currentRoute = Routes.ReviewHistory.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )

        assertFalse(
            shouldRestoreTabState(
                currentRoute = Routes.ReviewHistory.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )

        assertFalse(
            shouldSaveTabState(
                currentRoute = Routes.ReviewHistory.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )
    }

    @Test
    fun homeTabFromAnotherTabSavesCurrentTabButDoesNotRestoreHomeState() {
        assertFalse(
            shouldResetHomeScrollOnTabClick(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )

        assertTrue(
            shouldSaveTabState(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )

        assertFalse(
            shouldRestoreTabState(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Home.route,
                tabRoutes = tabRoutes
            )
        )
    }

    @Test
    fun nonHomeTabFromAnotherTabSavesAndRestoresTabState() {
        assertTrue(
            shouldSaveTabState(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Goals.route,
                tabRoutes = tabRoutes
            )
        )

        assertTrue(
            shouldRestoreTabState(
                currentRoute = Routes.Tasks.route,
                targetRoute = Routes.Goals.route,
                tabRoutes = tabRoutes
            )
        )
    }

    @Test
    fun reviewTabFromHomeSavesAndRestoresLikeANormalTab() {
        assertFalse(
            shouldResetHomeScrollOnTabClick(
                currentRoute = Routes.Home.route,
                targetRoute = Routes.Review.route,
                tabRoutes = tabRoutes
            )
        )

        assertTrue(
            shouldSaveTabState(
                currentRoute = Routes.Home.route,
                targetRoute = Routes.Review.route,
                tabRoutes = tabRoutes
            )
        )

        assertTrue(
            shouldRestoreTabState(
                currentRoute = Routes.Home.route,
                targetRoute = Routes.Review.route,
                tabRoutes = tabRoutes
            )
        )
    }
}
