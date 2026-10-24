package com.discipline.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    data object Today : Screen("today", "Today", Icons.Default.CheckCircle)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object Statistics : Screen("statistics", "Stats", Icons.Default.QueryStats)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    data object HabitForm : Screen("habit_form?habitId={habitId}") {
        fun createRoute(habitId: String? = null): String {
            return if (habitId != null) "habit_form?habitId=$habitId" else "habit_form"
        }
    }

    data object DayDetails : Screen("day_details/{date}") {
        fun createRoute(date: String): String = "day_details/$date"
    }

    data object Onboarding : Screen("onboarding", "Welcome")
    data object Auth : Screen("auth", "Account")
}

val bottomNavScreens = listOf(
    Screen.Today,
    Screen.Calendar,
    Screen.Statistics,
    Screen.Settings
)
