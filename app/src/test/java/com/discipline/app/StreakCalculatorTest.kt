package com.discipline.app

import com.discipline.app.data.model.DailyScore
import com.discipline.app.domain.DisciplineScoringEngine
import com.discipline.app.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun testStreak_UnscheduledDaysDoNotBreakStreak() {
        val scores = listOf(
            DailyScore("2026-10-18", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-19", 80f, 4, 5, DisciplineScoringEngine.COLOR_80_99),
            DailyScore("2026-10-20", null, 0, 0, DisciplineScoringEngine.COLOR_NONE), // Rest day: unscheduled
            DailyScore("2026-10-21", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-22", 100f, 3, 3, DisciplineScoringEngine.COLOR_100)
        )

        val result = StreakCalculator.calculateStreaks(
            scoresChronological = scores,
            streakThreshold = 80,
            todayDate = "2026-10-22"
        )

        // 4 active days meet or exceed threshold, 1 rest day is ignored
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.bestStreak)
        assertEquals(4, result.successfulDaysCount)
    }

    @Test
    fun testStreak_LowScoreBreaksStreak() {
        val scores = listOf(
            DailyScore("2026-10-18", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-19", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-20", 40f, 2, 5, DisciplineScoringEngine.COLOR_40_59), // Below 80% -> breaks!
            DailyScore("2026-10-21", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-22", 100f, 3, 3, DisciplineScoringEngine.COLOR_100)
        )

        val result = StreakCalculator.calculateStreaks(
            scoresChronological = scores,
            streakThreshold = 80,
            todayDate = "2026-10-22"
        )

        // Old streak was 2. Broke on Oct 20. New streak is 2 (Oct 21 and 22).
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.bestStreak)
        assertEquals(4, result.successfulDaysCount)
    }

    @Test
    fun testStreak_TodayInProgressDoesNotBreakPrematurely() {
        val scores = listOf(
            DailyScore("2026-10-20", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-21", 100f, 3, 3, DisciplineScoringEngine.COLOR_100),
            DailyScore("2026-10-22", 0f, 0, 3, DisciplineScoringEngine.COLOR_0) // Today: 0% so far
        )

        val result = StreakCalculator.calculateStreaks(
            scoresChronological = scores,
            streakThreshold = 80,
            todayDate = "2026-10-22"
        )

        // User hasn't finished today yet, so current streak remains 2 from yesterday!
        assertEquals(2, result.currentStreak)
    }
}
