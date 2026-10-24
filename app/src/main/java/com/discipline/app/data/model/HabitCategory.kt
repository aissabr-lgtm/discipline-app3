package com.discipline.app.data.model

enum class HabitCategory(val displayName: String) {
    HEALTH("Health"),
    STUDY("Study"),
    FITNESS("Fitness"),
    PERSONAL("Personal"),
    PRODUCTIVITY("Productivity"),
    CUSTOM("Custom");

    companion object {
        fun fromString(value: String): HabitCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: CUSTOM
        }
    }
}
