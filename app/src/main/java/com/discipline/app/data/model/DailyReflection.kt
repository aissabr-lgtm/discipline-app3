package com.discipline.app.data.model

data class DailyReflection(
    val date: String, // Format: YYYY-MM-DD
    val ownerUid: String = "",
    val reflectionText: String = "",
    val moodRating: Int = 0, // 0 = unrated, 1..5
    val updatedAt: Long = System.currentTimeMillis()
)
