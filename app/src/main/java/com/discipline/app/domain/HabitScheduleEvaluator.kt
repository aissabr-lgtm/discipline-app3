package com.discipline.app.domain

import com.discipline.app.data.model.Habit
import java.time.LocalDate

object HabitScheduleEvaluator {

    /**
     * Checks if a habit is active and scheduled on a given date.
     * Respects paused, archived, weekday selection, and creation date.
     */
    fun isScheduledOnDate(habit: Habit, date: LocalDate): Boolean {
        if (habit.isPaused || habit.isArchived) return false

        // Do not back-date habits before their creation date (unless created on same day)
        val createdLocalDate = try {
            val instant = java.time.Instant.ofEpochMilli(habit.createdAt)
            instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) {
            LocalDate.MIN
        }
        if (date.isBefore(createdLocalDate)) {
            return false
        }

        val dayOfWeekValue = date.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        return habit.scheduledDays.contains(dayOfWeekValue)
    }

    /**
     * Filters a list of habits down to only those scheduled on a given date.
     */
    fun filterScheduledHabits(habits: List<Habit>, date: LocalDate): List<Habit> {
        return habits.filter { isScheduledOnDate(it, date) }
    }
}
