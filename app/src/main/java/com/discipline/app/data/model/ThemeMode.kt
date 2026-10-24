package com.discipline.app.data.model

enum class ThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode");

    companion object {
        fun fromString(value: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}
