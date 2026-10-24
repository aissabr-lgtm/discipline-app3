package com.discipline.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.discipline.app.data.model.HabitLog

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["ownerUid", "date"])
    ]
)
data class HabitLogEntity(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val ownerUid: String,
    val date: String,
    val progressValue: Double,
    val isCompleted: Boolean,
    val updatedAt: Long
) {
    fun toDomain(): HabitLog = HabitLog(
        id = id,
        habitId = habitId,
        ownerUid = ownerUid,
        date = date,
        progressValue = progressValue,
        isCompleted = isCompleted,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(log: HabitLog): HabitLogEntity = HabitLogEntity(
            id = if (log.id.isNotBlank()) log.id else HabitLog.makeId(log.habitId, log.date),
            habitId = log.habitId,
            ownerUid = log.ownerUid,
            date = log.date,
            progressValue = log.progressValue,
            isCompleted = log.isCompleted,
            updatedAt = log.updatedAt
        )
    }
}
