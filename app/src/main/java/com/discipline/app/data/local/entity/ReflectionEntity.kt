package com.discipline.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.discipline.app.data.model.DailyReflection

@Entity(
    tableName = "reflections",
    indices = [
        Index(value = ["ownerUid", "date"], unique = true)
    ]
)
data class ReflectionEntity(
    @PrimaryKey
    val id: String,
    val ownerUid: String,
    val date: String,
    val reflectionText: String,
    val moodRating: Int,
    val updatedAt: Long
) {
    fun toDomain(): DailyReflection = DailyReflection(
        date = date,
        ownerUid = ownerUid,
        reflectionText = reflectionText,
        moodRating = moodRating,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(reflection: DailyReflection): ReflectionEntity = ReflectionEntity(
            id = "${reflection.ownerUid}_${reflection.date}",
            ownerUid = reflection.ownerUid,
            date = reflection.date,
            reflectionText = reflection.reflectionText,
            moodRating = reflection.moodRating,
            updatedAt = reflection.updatedAt
        )
    }
}
