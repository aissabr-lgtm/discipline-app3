package com.discipline.app

import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitCategory
import com.discipline.app.data.model.HabitLog
import com.discipline.app.data.model.HabitType
import com.discipline.app.domain.DisciplineScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisciplineScoringEngineTest {

    @Test
    fun testScoreCalculation_100Percent() {
        val score = DisciplineScoringEngine.calculateScore(completedCount = 5, scheduledCount = 5)
        assertEquals(100f, score)
    }

    @Test
    fun testScoreCalculation_Partial() {
        val score = DisciplineScoringEngine.calculateScore(completedCount = 4, scheduledCount = 5)
        assertEquals(80f, score)

        val score3 = DisciplineScoringEngine.calculateScore(completedCount = 3, scheduledCount = 5)
        assertEquals(60f, score3)

        val score1 = DisciplineScoringEngine.calculateScore(completedCount = 1, scheduledCount = 5)
        assertEquals(20f, score1)
    }

    @Test
    fun testScoreCalculation_ZeroPercent() {
        val score = DisciplineScoringEngine.calculateScore(completedCount = 0, scheduledCount = 5)
        assertEquals(0f, score)
    }

    @Test
    fun testScoreCalculation_NoScheduledHabits_ReturnsNull() {
        val score = DisciplineScoringEngine.calculateScore(completedCount = 0, scheduledCount = 0)
        assertNull(score)
    }

    @Test
    fun testComputeDailyScore_MeasurableAndYesNoHabits() {
        val habitYesNo = Habit(
            id = "h1",
            title = "Read",
            type = HabitType.YES_NO
        )
        val habitMeasurable = Habit(
            id = "h2",
            title = "Water",
            type = HabitType.MEASURABLE,
            targetValue = 2.0,
            unit = "litres"
        )

        val scheduled = listOf(habitYesNo, habitMeasurable)

        // Case 1: Both completed
        val logsBothDone = mapOf(
            "h1" to HabitLog(habitId = "h1", date = "2026-10-23", isCompleted = true),
            "h2" to HabitLog(habitId = "h2", date = "2026-10-23", progressValue = 2.0)
        )
        val scoreBoth = DisciplineScoringEngine.computeDailyScore("2026-10-23", scheduled, logsBothDone)
        assertEquals(100f, scoreBoth.score)
        assertEquals(2, scoreBoth.completedCount)
        assertEquals(2, scoreBoth.scheduledCount)
        assertEquals(DisciplineScoringEngine.COLOR_100, scoreBoth.colorHex)

        // Case 2: Measurable partial (1.5 / 2.0 litres -> not completed)
        val logsPartial = mapOf(
            "h1" to HabitLog(habitId = "h1", date = "2026-10-23", isCompleted = true),
            "h2" to HabitLog(habitId = "h2", date = "2026-10-23", progressValue = 1.5)
        )
        val scorePartial = DisciplineScoringEngine.computeDailyScore("2026-10-23", scheduled, logsPartial)
        assertEquals(50f, scorePartial.score)
        assertEquals(1, scorePartial.completedCount)
        assertEquals(DisciplineScoringEngine.COLOR_40_59, scorePartial.colorHex)
    }
}
