package com.discipline.app.data.model

data class HabitLog(
    val id: String = "",
    val habitId: String,
    val ownerUid: String = "",
    val date: String, // Format: YYYY-MM-DD
    val progressValue: Double = 0.0,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun makeId(habitId: String, date: String): String = "${habitId}_$date"
    }
}
