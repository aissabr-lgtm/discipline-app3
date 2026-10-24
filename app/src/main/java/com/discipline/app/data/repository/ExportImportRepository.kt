package com.discipline.app.data.repository

import com.discipline.app.data.local.AppDatabase
import com.discipline.app.data.local.entity.HabitEntity
import com.discipline.app.data.local.entity.HabitLogEntity
import com.discipline.app.data.local.entity.ReflectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ExportImportRepository(
    private val database: AppDatabase
) {
    suspend fun exportDataToJson(uid: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Discipline")
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("ownerUid", uid)

        // 1. Habits
        val habits = database.habitDao().getAllHabits(uid)
        val habitsArray = JSONArray()
        for (h in habits) {
            val hObj = JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("description", h.description)
                put("category", h.category)
                if (h.customCategory != null) put("customCategory", h.customCategory)
                put("type", h.type)
                put("targetValue", h.targetValue)
                put("unit", h.unit)
                put("scheduledDaysCsv", h.scheduledDaysCsv)
                if (h.reminderHour != null) put("reminderHour", h.reminderHour)
                if (h.reminderMinute != null) put("reminderMinute", h.reminderMinute)
                put("isPaused", h.isPaused)
                put("isArchived", h.isArchived)
                put("createdAt", h.createdAt)
                put("updatedAt", h.updatedAt)
            }
            habitsArray.put(hObj)
        }
        root.put("habits", habitsArray)

        // 2. Logs
        val logs = database.habitLogDao().getAllLogs(uid)
        val logsArray = JSONArray()
        for (l in logs) {
            val lObj = JSONObject().apply {
                put("id", l.id)
                put("habitId", l.habitId)
                put("date", l.date)
                put("progressValue", l.progressValue)
                put("isCompleted", l.isCompleted)
                put("updatedAt", l.updatedAt)
            }
            logsArray.put(lObj)
        }
        root.put("logs", logsArray)

        // 3. Reflections
        val reflections = database.reflectionDao().getAllReflections(uid)
        val reflectionsArray = JSONArray()
        for (r in reflections) {
            val rObj = JSONObject().apply {
                put("date", r.date)
                put("reflectionText", r.reflectionText)
                put("moodRating", r.moodRating)
                put("updatedAt", r.updatedAt)
            }
            reflectionsArray.put(rObj)
        }
        root.put("reflections", reflectionsArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String, currentUid: String, replaceExisting: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 0)
            if (version < 1) {
                return@withContext Result.failure(Exception("Unsupported or invalid export file version"))
            }

            if (replaceExisting) {
                database.habitDao().deleteAllForUser(currentUid)
                database.habitLogDao().deleteAllForUser(currentUid)
                database.reflectionDao().deleteAllForUser(currentUid)
            }

            var importedCount = 0

            // 1. Habits
            val habitsArray = root.optJSONArray("habits")
            if (habitsArray != null) {
                val habits = mutableListOf<HabitEntity>()
                for (i in 0 until habitsArray.length()) {
                    val obj = habitsArray.getJSONObject(i)
                    habits.add(
                        HabitEntity(
                            id = obj.getString("id"),
                            ownerUid = currentUid,
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "PERSONAL"),
                            customCategory = if (obj.has("customCategory")) obj.getString("customCategory") else null,
                            type = obj.optString("type", "YES_NO"),
                            targetValue = obj.optDouble("targetValue", 1.0),
                            unit = obj.optString("unit", ""),
                            scheduledDaysCsv = obj.optString("scheduledDaysCsv", "1,2,3,4,5,6,7"),
                            reminderHour = if (obj.has("reminderHour")) obj.getInt("reminderHour") else null,
                            reminderMinute = if (obj.has("reminderMinute")) obj.getInt("reminderMinute") else null,
                            isPaused = obj.optBoolean("isPaused", false),
                            isArchived = obj.optBoolean("isArchived", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                    importedCount++
                }
                database.habitDao().upsertHabits(habits)
            }

            // 2. Logs
            val logsArray = root.optJSONArray("logs")
            if (logsArray != null) {
                val logs = mutableListOf<HabitLogEntity>()
                for (i in 0 until logsArray.length()) {
                    val obj = logsArray.getJSONObject(i)
                    logs.add(
                        HabitLogEntity(
                            id = obj.getString("id"),
                            habitId = obj.getString("habitId"),
                            ownerUid = currentUid,
                            date = obj.getString("date"),
                            progressValue = obj.optDouble("progressValue", 0.0),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                database.habitLogDao().upsertLogs(logs)
            }

            // 3. Reflections
            val reflectionsArray = root.optJSONArray("reflections")
            if (reflectionsArray != null) {
                val reflections = mutableListOf<ReflectionEntity>()
                for (i in 0 until reflectionsArray.length()) {
                    val obj = reflectionsArray.getJSONObject(i)
                    val date = obj.getString("date")
                    reflections.add(
                        ReflectionEntity(
                            id = "${currentUid}_$date",
                            ownerUid = currentUid,
                            date = date,
                            reflectionText = obj.optString("reflectionText", ""),
                            moodRating = obj.optInt("moodRating", 0),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                database.reflectionDao().upsertReflections(reflections)
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse import data: ${e.localizedMessage}"))
        }
    }
}
