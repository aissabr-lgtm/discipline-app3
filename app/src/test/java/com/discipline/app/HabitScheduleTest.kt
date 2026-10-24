package com.discipline.app

import com.discipline.app.data.model.Habit
import com.discipline.app.domain.HabitScheduleEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitScheduleTest {

    @Test
    fun testHabitSchedule_Weekdays() {
        // Scheduled on Monday (1), Wednesday (3), Friday (5)
        val habit = Habit(
            id = "mwf",
            title = "Gym",
            scheduledDays = setOf(1, 3, 5),
            createdAt = 0L
        )

        val monday = LocalDate.of(2026, 10, 19)   // Monday
        val tuesday = LocalDate.of(2026, 10, 20)  // Tuesday
        val wednesday = LocalDate.of(2026, 10, 21)// Wednesday
        val thursday = LocalDate.of(2026, 10, 22) // Thursday
        val friday = LocalDate.of(2026, 10, 23)   // Friday

        assertTrue(HabitScheduleEvaluator.isScheduledOnDate(habit, monday))
        assertFalse(HabitScheduleEvaluator.isScheduledOnDate(habit, tuesday))
        assertTrue(HabitScheduleEvaluator.isScheduledOnDate(habit, wednesday))
        assertFalse(HabitScheduleEvaluator.isScheduledOnDate(habit, thursday))
        assertTrue(HabitScheduleEvaluator.isScheduledOnDate(habit, friday))
    }

    @Test
    fun testHabitSchedule_PausedAndArchived() {
        val habitActive = Habit(id = "1", title = "H1", isPaused = false, isArchived = false, createdAt = 0L)
        val habitPaused = Habit(id = "2", title = "H2", isPaused = true, isArchived = false, createdAt = 0L)
        val habitArchived = Habit(id = "3", title = "H3", isPaused = false, isArchived = true, createdAt = 0L)

        val today = LocalDate.now()

        assertTrue(HabitScheduleEvaluator.isScheduledOnDate(habitActive, today))
        assertFalse(HabitScheduleEvaluator.isScheduledOnDate(habitPaused, today))
        assertFalse(HabitScheduleEvaluator.isScheduledOnDate(habitArchived, today))
    }
}
