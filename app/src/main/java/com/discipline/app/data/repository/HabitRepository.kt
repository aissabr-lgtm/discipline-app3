package com.discipline.app.data.repository

import com.discipline.app.data.local.AppDatabase
import com.discipline.app.data.local.entity.HabitEntity
import com.discipline.app.data.local.entity.HabitLogEntity
import com.discipline.app.data.local.entity.ReflectionEntity
import com.discipline.app.data.local.entity.SyncQueueEntity
import com.discipline.app.data.model.DailyReflection
import com.discipline.app.data.model.DailyScore
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitLog
import com.discipline.app.data.remote.SyncManager
import com.discipline.app.domain.DisciplineScoringEngine
import com.discipline.app.domain.HabitScheduleEvaluator
import com.discipline.app.domain.StreakCalculator
import com.discipline.app.domain.StreakResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

data class StatisticsData(
    val todayScore: DailyScore,
    val weeklyCompletionRate: Float?,
    val monthlyCompletionRate: Float?,
    val streakResult: StreakResult,
    val totalCompletedHabitInstances: Int,
    val habitPerformanceList: List<HabitPerformanceSummary>
)

data class HabitPerformanceSummary(
    val habit: Habit,
    val completedCount: Int,
    val scheduledCount: Int,
    val completionRate: Float?
)

class HabitRepository(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) {
    // ----------------------------------------------------
    // Habits
    // ----------------------------------------------------
    fun observeActiveHabits(uid: String): Flow<List<Habit>> {
        return database.habitDao().observeActiveHabits(uid)
            .map { list -> list.map { it.toDomain() } }
    }

    fun observeAllHabits(uid: String): Flow<List<Habit>> {
        return database.habitDao().observeAllHabits(uid)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getHabitById(id: String): Habit? = withContext(Dispatchers.IO) {
        database.habitDao().getHabitById(id)?.toDomain()
    }

    suspend fun saveHabit(habit: Habit): Unit = withContext(Dispatchers.IO) {
        val entity = HabitEntity.fromDomain(habit)
        database.habitDao().upsertHabit(entity)

        // Enqueue cloud sync
        val json = JSONObject().apply {
            put("id", habit.id)
            put("ownerUid", habit.ownerUid)
            put("title", habit.title)
            put("description", habit.description)
            put("category", habit.category.name)
            put("customCategory", habit.customCategory)
            put("type", habit.type.name)
            put("targetValue", habit.targetValue)
            put("unit", habit.unit)
            put("scheduledDaysCsv", habit.scheduledDays.sorted().joinToString(","))
            if (habit.reminderHour != null) put("reminderHour", habit.reminderHour)
            if (habit.reminderMinute != null) put("reminderMinute", habit.reminderMinute)
            put("isPaused", habit.isPaused)
            put("isArchived", habit.isArchived)
            put("createdAt", habit.createdAt)
            put("updatedAt", habit.updatedAt)
        }
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                ownerUid = habit.ownerUid,
                entityType = "HABIT",
                entityId = habit.id,
                action = "UPSERT",
                payloadJson = json.toString()
            )
        )
        // Background sync trigger
        syncManager.synchronize(habit.ownerUid)
    }

    suspend fun setHabitPaused(id: String, isPaused: Boolean, uid: String): Unit = withContext(Dispatchers.IO) {
        database.habitDao().setPaused(id, isPaused)
        val habit = database.habitDao().getHabitById(id)?.toDomain()
        if (habit != null) {
            saveHabit(habit.copy(isPaused = isPaused, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun setHabitArchived(id: String, isArchived: Boolean, uid: String): Unit = withContext(Dispatchers.IO) {
        database.habitDao().setArchived(id, isArchived)
        val habit = database.habitDao().getHabitById(id)?.toDomain()
        if (habit != null) {
            saveHabit(habit.copy(isArchived = isArchived, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteHabit(id: String, uid: String): Unit = withContext(Dispatchers.IO) {
        database.habitDao().deleteHabitById(id)
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                ownerUid = uid,
                entityType = "HABIT",
                entityId = id,
                action = "DELETE",
                payloadJson = "{}"
            )
        )
        syncManager.synchronize(uid)
    }

    // ----------------------------------------------------
    // Habit Logs & Progress
    // ----------------------------------------------------
    fun observeLogsForDate(uid: String, date: String): Flow<Map<String, HabitLog>> {
        return database.habitLogDao().observeLogsForDate(uid, date)
            .map { list -> list.associate { it.habitId to it.toDomain() } }
    }

    suspend fun getLogsForDate(uid: String, date: String): Map<String, HabitLog> = withContext(Dispatchers.IO) {
        database.habitLogDao().getLogsForDate(uid, date).associate { it.habitId to it.toDomain() }
    }

    suspend fun updateHabitProgress(
        habitId: String,
        date: String,
        progressValue: Double,
        isCompleted: Boolean,
        uid: String
    ): Unit = withContext(Dispatchers.IO) {
        val log = HabitLog(
            id = HabitLog.makeId(habitId, date),
            habitId = habitId,
            ownerUid = uid,
            date = date,
            progressValue = progressValue,
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        database.habitLogDao().upsertLog(HabitLogEntity.fromDomain(log))

        val json = JSONObject().apply {
            put("id", log.id)
            put("habitId", log.habitId)
            put("ownerUid", uid)
            put("date", log.date)
            put("progressValue", log.progressValue)
            put("isCompleted", log.isCompleted)
            put("updatedAt", log.updatedAt)
        }
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                ownerUid = uid,
                entityType = "LOG",
                entityId = log.id,
                action = "UPSERT",
                payloadJson = json.toString()
            )
        )
        syncManager.synchronize(uid)
    }

    suspend fun toggleYesNoHabit(habitId: String, date: String, uid: String): Unit = withContext(Dispatchers.IO) {
        val existing = database.habitLogDao().getLog(habitId, date)?.toDomain()
        val currentlyCompleted = existing?.isCompleted == true
        val newCompleted = !currentlyCompleted
        val newProgress = if (newCompleted) 1.0 else 0.0
        updateHabitProgress(habitId, date, newProgress, newCompleted, uid)
    }

    // ----------------------------------------------------
    // Daily Score Computation
    // ----------------------------------------------------
    fun observeDailyScore(uid: String, date: String): Flow<DailyScore> {
        val localDate = LocalDate.parse(date)
        return combine(
            database.habitDao().observeActiveHabits(uid),
            database.habitLogDao().observeLogsForDate(uid, date)
        ) { habitEntities, logEntities ->
            val allHabits = habitEntities.map { it.toDomain() }
            val scheduled = HabitScheduleEvaluator.filterScheduledHabits(allHabits, localDate)
            val logsMap = logEntities.associate { it.habitId to it.toDomain() }
            DisciplineScoringEngine.computeDailyScore(date, scheduled, logsMap)
        }
    }

    suspend fun getDailyScoreForDate(uid: String, date: String): DailyScore = withContext(Dispatchers.IO) {
        val localDate = LocalDate.parse(date)
        val habits = database.habitDao().getActiveHabits(uid).map { it.toDomain() }
        val scheduled = HabitScheduleEvaluator.filterScheduledHabits(habits, localDate)
        val logsMap = database.habitLogDao().getLogsForDate(uid, date).associate { it.habitId to it.toDomain() }
        DisciplineScoringEngine.computeDailyScore(date, scheduled, logsMap)
    }

    suspend fun getDailyScoresForMonth(uid: String, yearMonth: YearMonth): List<DailyScore> = withContext(Dispatchers.IO) {
        val habits = database.habitDao().getActiveHabits(uid).map { it.toDomain() }
        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()
        val allMonthLogs = database.habitLogDao().getLogsForDateRange(uid, startDate, endDate)
        val logsByDate = allMonthLogs.groupBy { it.date }

        val scores = mutableListOf<DailyScore>()
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dateStr = date.toString()
            val scheduled = HabitScheduleEvaluator.filterScheduledHabits(habits, date)
            val dayLogs = (logsByDate[dateStr] ?: emptyList()).associate { it.habitId to it.toDomain() }
            scores.add(DisciplineScoringEngine.computeDailyScore(dateStr, scheduled, dayLogs))
        }
        scores
    }

    // ----------------------------------------------------
    // Daily Reflections
    // ----------------------------------------------------
    fun observeReflection(uid: String, date: String): Flow<DailyReflection?> {
        return database.reflectionDao().observeReflection(uid, date)
            .map { it?.toDomain() }
    }

    suspend fun getReflection(uid: String, date: String): DailyReflection? = withContext(Dispatchers.IO) {
        database.reflectionDao().getReflection(uid, date)?.toDomain()
    }

    suspend fun saveReflection(reflection: DailyReflection): Unit = withContext(Dispatchers.IO) {
        database.reflectionDao().upsertReflection(ReflectionEntity.fromDomain(reflection))
        val json = JSONObject().apply {
            put("date", reflection.date)
            put("ownerUid", reflection.ownerUid)
            put("reflectionText", reflection.reflectionText)
            put("moodRating", reflection.moodRating)
            put("updatedAt", reflection.updatedAt)
        }
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                ownerUid = reflection.ownerUid,
                entityType = "REFLECTION",
                entityId = reflection.date,
                action = "UPSERT",
                payloadJson = json.toString()
            )
        )
        syncManager.synchronize(reflection.ownerUid)
    }

    // ----------------------------------------------------
    // Statistics & Streaks
    // ----------------------------------------------------
    suspend fun getStatistics(uid: String, threshold: Int): StatisticsData = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val habits = database.habitDao().getActiveHabits(uid).map { it.toDomain() }

        // Last 60 days for streak calculation
        val scoresList = mutableListOf<DailyScore>()
        for (i in 59 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            val scheduled = HabitScheduleEvaluator.filterScheduledHabits(habits, date)
            val logs = database.habitLogDao().getLogsForDate(uid, dateStr).associate { it.habitId to it.toDomain() }
            scoresList.add(DisciplineScoringEngine.computeDailyScore(dateStr, scheduled, logs))
        }

        val streakResult = StreakCalculator.calculateStreaks(scoresList, threshold, todayStr)
        val todayScore = scoresList.last()

        // Weekly completion (last 7 days)
        val last7Days = scoresList.takeLast(7).filter { it.score != null }
        val weeklyRate = if (last7Days.isNotEmpty()) {
            last7Days.map { it.score!! }.average().toFloat()
        } else null

        // Monthly completion (last 30 days)
        val last30Days = scoresList.takeLast(30).filter { it.score != null }
        val monthlyRate = if (last30Days.isNotEmpty()) {
            last30Days.map { it.score!! }.average().toFloat()
        } else null

        // Total completed habit logs
        val allLogs = database.habitLogDao().getAllLogs(uid)
        val totalCompletedCount = allLogs.count { it.isCompleted }

        // Habit performance summaries (last 30 days)
        val habitSummaries = habits.map { habit ->
            var scheduledDaysCount = 0
            var completedDaysCount = 0
            for (i in 29 downTo 0) {
                val date = today.minusDays(i.toLong())
                if (HabitScheduleEvaluator.isScheduledOnDate(habit, date)) {
                    scheduledDaysCount++
                    val log = database.habitLogDao().getLog(habit.id, date.toString())
                    val isDone = when (habit.type) {
                        com.discipline.app.data.model.HabitType.YES_NO -> log?.isCompleted == true
                        com.discipline.app.data.model.HabitType.MEASURABLE -> (log?.progressValue ?: 0.0) >= habit.targetValue
                    }
                    if (isDone) completedDaysCount++
                }
            }
            val rate = if (scheduledDaysCount > 0) {
                (completedDaysCount.toFloat() / scheduledDaysCount.toFloat()) * 100f
            } else null

            HabitPerformanceSummary(
                habit = habit,
                completedCount = completedDaysCount,
                scheduledCount = scheduledDaysCount,
                completionRate = rate
            )
        }

        StatisticsData(
            todayScore = todayScore,
            weeklyCompletionRate = weeklyRate,
            monthlyCompletionRate = monthlyRate,
            streakResult = streakResult,
            totalCompletedHabitInstances = totalCompletedCount,
            habitPerformanceList = habitSummaries
        )
    }
}
