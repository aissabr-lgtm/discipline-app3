package com.discipline.app.data.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val streakThreshold: Int = 80,
    val isOnboardingCompleted: Boolean = false,
    val notificationsEnabled: Boolean = true
)
