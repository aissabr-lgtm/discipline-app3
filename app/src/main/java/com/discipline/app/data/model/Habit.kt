package com.discipline.app.data.model

import java.util.UUID

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val ownerUid: String = "",
    val title: String,
    val description: String = "",
    val category: HabitCategory = HabitCategory.PERSONAL,
    val customCategory: String? = null,
    val type: HabitType = HabitType.YES_NO,
    val targetValue: Double = 1.0,
    val unit: String = "",
    val scheduledDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon, 7=Sun (java.time.DayOfWeek.value)
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val categoryName: String
        get() = if (category == HabitCategory.CUSTOM && !customCategory.isNullOrBlank()) {
            customCategory
        } else {
            category.displayName
        }

    val hasReminder: Boolean
        get() = reminderHour != null && reminderMinute != null

    val formattedReminderTime: String?
        get() = if (hasReminder) {
            val h = reminderHour!!
            val m = reminderMinute!!
            String.format("%02d:%02d", h, m)
        } else null

    fun isScheduledForDay(dayOfWeekValue: Int): Boolean {
        if (isPaused || isArchived) return false
        return scheduledDays.contains(dayOfWeekValue)
    }
}
