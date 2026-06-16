package com.jimu.app.navigation

sealed class Routes(val route: String, val title: String) {
    data object Home : Routes("home", "首页")
    data object Tasks : Routes("tasks", "待办")
    data object Habits : Routes("habits", "习惯")
    data object Goals : Routes("goals", "目标")
    data object Completed : Routes("completed", "已完成")
    data object Review : Routes("review", "今日复盘")
    data object ReviewHistory : Routes("review-history", "复盘历史")
    data object ReviewByDate : Routes("review/{reviewDate}", "复盘记录") {
        const val ARG_REVIEW_DATE = "reviewDate"

        fun createRoute(reviewDate: String): String {
            return "review/$reviewDate"
        }
    }
}
