package com.discipline.app.domain

import com.discipline.app.data.model.DailyScore
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitLog

object DisciplineScoringEngine {

    // Exact color hex constants from specification
    const val COLOR_100: Long = 0xFF064E3BL      // Dark green: 100%
    const val COLOR_80_99: Long = 0xFF10B981L    // Green: 80–99%
    const val COLOR_60_79: Long = 0xFF84CC16L    // Lime green: 60–79%
    const val COLOR_40_59: Long = 0xFFF59E0BL    // Amber: 40–59%
    const val COLOR_20_39: Long = 0xFFF97316L    // Orange: 20–39%
    const val COLOR_1_19: Long = 0xFFEF4444L     // Red: 1–19%
    const val COLOR_0: Long = 0xFF7F1D1DL        // Dark red: 0%
    const val COLOR_NONE: Long = 0xFF6B7280L     // Neutral gray: No scheduled habits

    /**
     * Calculates the daily score percentage:
     * (Number of completed scheduled habits / Number of scheduled active habits) * 100
     * Returns null if no habits are scheduled.
     */
    fun calculateScore(completedCount: Int, scheduledCount: Int): Float? {
        if (scheduledCount <= 0) return null
        val boundedCompleted = completedCount.coerceIn(0, scheduledCount)
        return (boundedCompleted.toFloat() / scheduledCount.toFloat()) * 100f
    }

    /**
     * Resolves the exact hex color for a given score.
     * null indicates no scheduled habits -> neutral gray (#6B7280).
     */
    fun getColorForScore(score: Float?): Long {
        if (score == null) return COLOR_NONE
        return when {
            score >= 100f -> COLOR_100
            score >= 80f -> COLOR_80_99
            score >= 60f -> COLOR_60_79
            score >= 40f -> COLOR_40_59
            score >= 20f -> COLOR_20_39
            score > 0f -> COLOR_1_19
            else -> COLOR_0
        }
    }

    /**
     * Computes the complete DailyScore for a specific date given scheduled habits and their logs.
     */
    fun computeDailyScore(
        date: String,
        scheduledHabits: List<Habit>,
        logsByHabitId: Map<String, HabitLog>
    ): DailyScore {
        val scheduledCount = scheduledHabits.size
        if (scheduledCount == 0) {
            return DailyScore(
                date = date,
                score = null,
                completedCount = 0,
                scheduledCount = 0,
                colorHex = COLOR_NONE
            )
        }

        var completedCount = 0
        for (habit in scheduledHabits) {
            val log = logsByHabitId[habit.id]
            val isCompleted = when (habit.type) {
                com.discipline.app.data.model.HabitType.YES_NO -> log?.isCompleted == true
                com.discipline.app.data.model.HabitType.MEASURABLE -> {
                    val progress = log?.progressValue ?: 0.0
                    progress >= habit.targetValue
                }
            }
            if (isCompleted) {
                completedCount++
            }
        }

        val score = calculateScore(completedCount, scheduledCount)
        val colorHex = getColorForScore(score)

        return DailyScore(
            date = date,
            score = score,
            completedCount = completedCount,
            scheduledCount = scheduledCount,
            colorHex = colorHex
        )
    }
}
