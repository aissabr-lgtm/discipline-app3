package com.discipline.app.data.remote

import android.util.Log
import com.discipline.app.data.local.AppDatabase
import com.discipline.app.data.local.entity.HabitEntity
import com.discipline.app.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SyncManager(
    private val database: AppDatabase,
    private val supabaseService: SupabaseService
) {
    private val tag = "SyncManager"

    suspend fun synchronize(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!supabaseService.isConfigured || !supabaseService.isUserAuthenticated || uid == "local_user") {
            return@withContext Result.success(Unit)
        }

        try {
            // 1. Drain pending sync queue
            val pendingItems = database.syncQueueDao().getPendingForUser(uid)
            for (item in pendingItems) {
                val success = processQueueItem(item)
                if (success) {
                    database.syncQueueDao().dequeue(item.queueId)
                }
            }

            // 2. Fetch remote habits and merge by updatedAt (newer record wins).
            val remoteHabitsResult = supabaseService.fetchHabitsFromCloud()
            if (remoteHabitsResult.isFailure) return@withContext Result.failure(remoteHabitsResult.exceptionOrNull()!!)
            for (remote in remoteHabitsResult.getOrThrow()) {
                val local = database.habitDao().getHabitById(remote.id)
                if (local == null || remote.updatedAt > local.updatedAt) {
                    database.habitDao().upsertHabit(HabitEntity.fromDomain(remote))
                }
            }

            // 3. Download logs too; keeping them local-only loses history on a new device.
            val remoteLogsResult = supabaseService.fetchHabitLogsFromCloud()
            if (remoteLogsResult.isFailure) return@withContext Result.failure(remoteLogsResult.exceptionOrNull()!!)
            for (remote in remoteLogsResult.getOrThrow()) {
                val local = database.habitLogDao().getLog(remote.habitId, remote.date)
                if (local == null || remote.updatedAt > local.updatedAt) {
                    database.habitLogDao().upsertLog(
                        com.discipline.app.data.local.entity.HabitLogEntity.fromDomain(remote)
                    )
                }
            }

            // 4. Download reflections and merge by owner/date, preserving the newer copy.
            val remoteReflectionsResult = supabaseService.fetchReflectionsFromCloud()
            if (remoteReflectionsResult.isFailure) return@withContext Result.failure(remoteReflectionsResult.exceptionOrNull()!!)
            for (remote in remoteReflectionsResult.getOrThrow()) {
                val local = database.reflectionDao().getReflection(uid, remote.date)
                if (local == null || remote.updatedAt > local.updatedAt) {
                    database.reflectionDao().upsertReflection(
                        com.discipline.app.data.local.entity.ReflectionEntity.fromDomain(remote)
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Sync error", e)
            Result.failure(e)
        }
    }

    private suspend fun processQueueItem(item: SyncQueueEntity): Boolean {
        return try {
            when (item.entityType) {
                "HABIT" -> {
                    if (item.action == "DELETE") {
                        supabaseService.deleteHabit(item.entityId).isSuccess
                    } else {
                        val json = JSONObject(item.payloadJson)
                        val habit = HabitEntity(
                            id = item.entityId,
                            ownerUid = item.ownerUid,
                            title = json.getString("title"),
                            description = json.optString("description", ""),
                            category = json.getString("category"),
                            customCategory = if (json.has("customCategory")) json.getString("customCategory") else null,
                            type = json.getString("type"),
                            targetValue = json.getDouble("targetValue"),
                            unit = json.optString("unit", ""),
                            scheduledDaysCsv = json.optString("scheduledDaysCsv", "1,2,3,4,5,6,7"),
                            reminderHour = if (json.has("reminderHour")) json.getInt("reminderHour") else null,
                            reminderMinute = if (json.has("reminderMinute")) json.getInt("reminderMinute") else null,
                            isPaused = json.optBoolean("isPaused", false),
                            isArchived = json.optBoolean("isArchived", false),
                            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
                        ).toDomain()
                        supabaseService.uploadHabit(habit).isSuccess
                    }
                }
                "LOG" -> {
                    val json = JSONObject(item.payloadJson)
                    val log = com.discipline.app.data.model.HabitLog(
                        id = item.entityId,
                        habitId = json.getString("habitId"),
                        ownerUid = item.ownerUid,
                        date = json.getString("date"),
                        progressValue = json.getDouble("progressValue"),
                        isCompleted = json.getBoolean("isCompleted"),
                        updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
                    )
                    supabaseService.uploadHabitLog(log).isSuccess
                }
                "REFLECTION" -> {
                    val json = JSONObject(item.payloadJson)
                    val reflection = com.discipline.app.data.model.DailyReflection(
                        date = item.entityId,
                        ownerUid = item.ownerUid,
                        reflectionText = json.getString("reflectionText"),
                        moodRating = json.getInt("moodRating"),
                        updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
                    )
                    supabaseService.uploadReflection(reflection).isSuccess
                }
                else -> true
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to process queue item ${item.queueId}", e)
            false
        }
    }
}
