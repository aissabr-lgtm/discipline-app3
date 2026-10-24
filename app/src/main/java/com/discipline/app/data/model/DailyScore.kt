package com.discipline.app.data.model

data class DailyScore(
    val date: String, // Format: YYYY-MM-DD
    val score: Float?, // null if scheduledCount == 0
    val completedCount: Int,
    val scheduledCount: Int,
    val colorHex: Long
) {
    val isCompleteDay: Boolean
        get() = score != null && score >= 100f

    val isSuccessfulDay: Boolean
        fun isSuccessful(threshold: Int = 80): Boolean = score != null && score >= threshold

    val formattedPercent: String
        get() = if (score != null) "${score.toInt()}%" else "N/A"

    val fractionText: String
        get() = "$completedCount / $scheduledCount completed"
}
