package com.discipline.app.domain

import com.discipline.app.data.model.DailyScore
import java.time.LocalDate

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val successfulDaysCount: Int
)

object StreakCalculator {

    /**
     * Calculates current streak and best streak across chronological scores.
     * @param scoresChronological List of DailyScore sorted in ascending date order (oldest to newest)
     * @param streakThreshold Completion threshold percentage to count as success (default 80%)
     * @param todayDate The current date in YYYY-MM-DD format
     */
    fun calculateStreaks(
        scoresChronological: List<DailyScore>,
        streakThreshold: Int = 80,
        todayDate: String = LocalDate.now().toString()
    ): StreakResult {
        if (scoresChronological.isEmpty()) {
            return StreakResult(currentStreak = 0, bestStreak = 0, successfulDaysCount = 0)
        }

        var runningStreak = 0
        var bestStreak = 0
        var totalSuccessDays = 0

        // Track history excluding today for calculating current streak if today is not yet successful
        var streakBeforeToday = 0

        for (daily in scoresChronological) {
            val score = daily.score
            val isToday = daily.date == todayDate

            if (score == null) {
                // No scheduled habits: does not break streak
                continue
            }

            if (score >= streakThreshold) {
                runningStreak++
                totalSuccessDays++
                if (runningStreak > bestStreak) {
                    bestStreak = runningStreak
                }
            } else {
                if (!isToday) {
                    // Past day below threshold breaks streak
                    runningStreak = 0
                }
            }

            if (!isToday) {
                streakBeforeToday = runningStreak
            }
        }

        // Evaluate current streak:
        // If today is present and has reached threshold, current streak is runningStreak.
        // If today is present and has not yet reached threshold, user still has time today,
        // so current streak remains streakBeforeToday.
        val todayDaily = scoresChronological.find { it.date == todayDate }
        val currentStreak = if (todayDaily != null) {
            val todayScore = todayDaily.score
            if (todayScore != null && todayScore >= streakThreshold) {
                runningStreak
            } else {
                streakBeforeToday
            }
        } else {
            runningStreak
        }

        return StreakResult(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            successfulDaysCount = totalSuccessDays
        )
    }
}
