package com.discipline.app

import com.discipline.app.domain.DisciplineScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorScaleTest {

    @Test
    fun testAllEightColorHexCodes() {
        // 100%: #064E3B
        assertEquals(0xFF064E3BL, DisciplineScoringEngine.getColorForScore(100f))

        // 80–99%: #10B981
        assertEquals(0xFF10B981L, DisciplineScoringEngine.getColorForScore(95f))
        assertEquals(0xFF10B981L, DisciplineScoringEngine.getColorForScore(80f))

        // 60–79%: #84CC16
        assertEquals(0xFF84CC16L, DisciplineScoringEngine.getColorForScore(79f))
        assertEquals(0xFF84CC16L, DisciplineScoringEngine.getColorForScore(60f))

        // 40–59%: #F59E0B
        assertEquals(0xFFF59E0BL, DisciplineScoringEngine.getColorForScore(50f))
        assertEquals(0xFFF59E0BL, DisciplineScoringEngine.getColorForScore(40f))

        // 20–39%: #F97316
        assertEquals(0xFFF97316L, DisciplineScoringEngine.getColorForScore(35f))
        assertEquals(0xFFF97316L, DisciplineScoringEngine.getColorForScore(20f))

        // 1–19%: #EF4444
        assertEquals(0xFFEF4444L, DisciplineScoringEngine.getColorForScore(15f))
        assertEquals(0xFFEF4444L, DisciplineScoringEngine.getColorForScore(1f))

        // 0%: #7F1D1D
        assertEquals(0xFF7F1D1DL, DisciplineScoringEngine.getColorForScore(0f))

        // No scheduled habits: #6B7280
        assertEquals(0xFF6B7280L, DisciplineScoringEngine.getColorForScore(null))
    }
}
