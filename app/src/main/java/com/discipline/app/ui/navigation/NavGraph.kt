
package com.discipline.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.discipline.app.DisciplineApplication
import com.discipline.app.ui.screens.auth.AuthScreen
import com.discipline.app.ui.screens.auth.AuthViewModel
import com.discipline.app.ui.screens.calendar.CalendarScreen
import com.discipline.app.ui.screens.calendar.CalendarViewModel
import com.discipline.app.ui.screens.daydetails.DayDetailsScreen
import com.discipline.app.ui.screens.daydetails.DayDetailsViewModel
import com.discipline.app.ui.screens.habit.HabitFormScreen
import com.discipline.app.ui.screens.habit.HabitFormViewModel
import com.discipline.app.ui.screens.onboarding.OnboardingScreen
import com.discipline.app.ui.screens.onboarding.OnboardingViewModel
import com.discipline.app.ui.screens.settings.SettingsScreen
import com.discipline.app.ui.screens.settings.SettingsViewModel
import com.discipline.app.ui.screens.statistics.StatisticsScreen
import com.discipline.app.ui.screens.statistics.StatisticsViewModel
import com.discipline.app.ui.screens.today.TodayScreen
import com.discipline.app.ui.screens.today.TodayViewModel

@Composable
fun DisciplineNavGraph(
    navController: NavHostController,
    app: DisciplineApplication,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (screen.icon != null) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Onboarding.route) {
                val vm = viewModel {
                    OnboardingViewModel(
                        app.preferencesRepository,
                        app.habitRepository,
                        app.authRepository
                    )
                }
                OnboardingScreen(
                    viewModel = vm,
                    onFinishOnboarding = {
                        navController.navigate(Screen.Today.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            composable(Screen.Auth.route) {
                val vm = viewModel { AuthViewModel(app.authRepository) }
                AuthScreen(
                    viewModel = vm,
                    onAuthSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Today.route) {
                val vm = viewModel {
                    TodayViewModel(app.habitRepository, app.authRepository)
                }
                TodayScreen(
                    viewModel = vm,
                    onNavigateToCreateHabit = {
                        navController.navigate(Screen.HabitForm.createRoute())
                    },
                    onNavigateToEditHabit = { habitId ->
                        navController.navigate(Screen.HabitForm.createRoute(habitId))
                    },
                    onNavigateToReflection = { date ->
                        navController.navigate(Screen.DayDetails.createRoute(date))
                    }
                )
            }

            composable(Screen.Calendar.route) {
                val vm = viewModel {
                    CalendarViewModel(app.habitRepository, app.authRepository)
                }
                CalendarScreen(
                    viewModel = vm,
                    onDateSelected = { date ->
                        navController.navigate(Screen.DayDetails.createRoute(date))
                    }
                )
            }

            composable(Screen.Statistics.route) {
                val vm = viewModel {
                    StatisticsViewModel(
                        app.habitRepository,
                        app.authRepository,
                        app.preferencesRepository
                    )
                }
                StatisticsScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm = viewModel {
                    SettingsViewModel(
                        app.preferencesRepository,
                        app.authRepository,
                        app.exportImportRepository,
                        app.supabaseService
                    )
                }
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            composable(
                route = Screen.HabitForm.route,
                arguments = listOf(
                    navArgument("habitId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString("habitId")
                val vm = viewModel(key = "HabitForm_${habitId ?: "new"}") {
                    HabitFormViewModel(
                        app.habitRepository,
                        app.authRepository,
                        app.notificationScheduler,
                        habitId
                    )
                }
                HabitFormScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.DayDetails.route,
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val vm = viewModel(key = "DayDetails_$date") {
                    DayDetailsViewModel(app.habitRepository, app.authRepository, date)
                }
                DayDetailsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
