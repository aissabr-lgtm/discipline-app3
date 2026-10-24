package com.discipline.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Palette
val LightBackground = Color(0xFFF8FAFC)
val LightCard = Color(0xFFFFFFFF)
val LightPrimary = Color(0xFF059669)
val LightTextMain = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)
val LightBorder = Color(0xFFE2E8F0)

// Dark Theme Palette
val DarkBackground = Color(0xFF0F172A)
val DarkCard = Color(0xFF1E293B)
val DarkPrimary = Color(0xFF10B981)
val DarkTextMain = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkBorder = Color(0xFF334155)

// Daily Score Performance Colors (Strictly identical in Light & Dark modes)
val ScoreColor100 = Color(0xFF064E3B)      // 100% Dark green
val ScoreColor80to99 = Color(0xFF10B981)   // 80–99% Green
val ScoreColor60to79 = Color(0xFF84CC16)   // 60–79% Lime green
val ScoreColor40to59 = Color(0xFFF59E0B)   // 40–59% Amber
val ScoreColor20to39 = Color(0xFFF97316)   // 20–39% Orange
val ScoreColor1to19 = Color(0xFFEF4444)    // 1–19% Red
val ScoreColor0 = Color(0xFF7F1D1D)        // 0% Dark red
val ScoreColorNone = Color(0xFF6B7280)     // No scheduled habits: Neutral gray

fun getScoreComposeColor(colorHex: Long): Color {
    return Color(colorHex)
}
