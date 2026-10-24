package com.discipline.app.data.model

enum class HabitType {
    YES_NO,
    MEASURABLE;

    val displayName: String
        get() = when (this) {
            YES_NO -> "Yes / No"
            MEASURABLE -> "Measurable"
        }
}
