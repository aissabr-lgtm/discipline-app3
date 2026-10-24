package com.discipline.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitCategory
import com.discipline.app.data.model.HabitType

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val ownerUid: String,
    val title: String,
    val description: String,
    val category: String,
    val customCategory: String?,
    val type: String,
    val targetValue: Double,
    val unit: String,
    val scheduledDaysCsv: String, // e.g. "1,2,3,4,5,6,7"
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val isPaused: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Habit {
        val days = if (scheduledDaysCsv.isBlank()) {
            emptySet()
        } else {
            scheduledDaysCsv.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        }

        return Habit(
            id = id,
            ownerUid = ownerUid,
            title = title,
            description = description,
            category = HabitCategory.fromString(category),
            customCategory = customCategory,
            type = try { HabitType.valueOf(type) } catch (_: Exception) { HabitType.YES_NO },
            targetValue = targetValue,
            unit = unit,
            scheduledDays = days,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            isPaused = isPaused,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(habit: Habit): HabitEntity {
            return HabitEntity(
                id = habit.id,
                ownerUid = habit.ownerUid,
                title = habit.title,
                description = habit.description,
                category = habit.category.name,
                customCategory = habit.customCategory,
                type = habit.type.name,
                targetValue = habit.targetValue,
                unit = habit.unit,
                scheduledDaysCsv = habit.scheduledDays.sorted().joinToString(","),
                reminderHour = habit.reminderHour,
                reminderMinute = habit.reminderMinute,
                isPaused = habit.isPaused,
                isArchived = habit.isArchived,
                createdAt = habit.createdAt,
                updatedAt = habit.updatedAt
            )
        }
    }
}
