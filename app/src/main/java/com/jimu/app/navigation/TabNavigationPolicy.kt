package com.jimu.app.navigation

internal fun shouldResetHomeScrollOnTabClick(
    currentRoute: String?,
    targetRoute: String,
    tabRoutes: Set<String>
): Boolean {
    return targetRoute == Routes.Home.route &&
            currentRoute != null &&
            currentRoute !in tabRoutes
}

internal fun shouldRestoreTabState(
    currentRoute: String?,
    targetRoute: String,
    tabRoutes: Set<String>
): Boolean {
    if (shouldResetHomeScrollOnTabClick(currentRoute, targetRoute, tabRoutes)) {
        return false
    }

    if (targetRoute == Routes.Home.route) {
        return false
    }

    return currentRoute in tabRoutes && targetRoute in tabRoutes
}

internal fun shouldSaveTabState(
    currentRoute: String?,
    targetRoute: String,
    tabRoutes: Set<String>
): Boolean {
    if (shouldResetHomeScrollOnTabClick(currentRoute, targetRoute, tabRoutes)) {
        return false
    }

    return currentRoute in tabRoutes && targetRoute in tabRoutes && currentRoute != targetRoute
}
