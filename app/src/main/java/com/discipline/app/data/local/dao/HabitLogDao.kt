package com.discipline.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.discipline.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE ownerUid = :ownerUid AND date = :date")
    fun observeLogsForDate(ownerUid: String, date: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE ownerUid = :ownerUid AND date = :date")
    suspend fun getLogsForDate(ownerUid: String, date: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE ownerUid = :ownerUid AND date BETWEEN :startDate AND :endDate")
    suspend fun getLogsForDateRange(ownerUid: String, startDate: String, endDate: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE ownerUid = :ownerUid AND date BETWEEN :startDate AND :endDate")
    fun observeLogsForDateRange(ownerUid: String, startDate: String, endDate: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    suspend fun getLogsForHabit(habitId: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: String, date: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE ownerUid = :ownerUid")
    fun observeAllLogs(ownerUid: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE ownerUid = :ownerUid")
    suspend fun getAllLogs(ownerUid: String): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: String, date: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: String)

    @Query("DELETE FROM habit_logs WHERE ownerUid = :ownerUid")
    suspend fun deleteAllForUser(ownerUid: String)
}
