package com.discipline.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.discipline.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE ownerUid = :ownerUid AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeActiveHabits(ownerUid: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE ownerUid = :ownerUid ORDER BY createdAt ASC")
    fun observeAllHabits(ownerUid: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE ownerUid = :ownerUid AND isArchived = 0 ORDER BY createdAt ASC")
    suspend fun getActiveHabits(ownerUid: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE ownerUid = :ownerUid ORDER BY createdAt ASC")
    suspend fun getAllHabits(ownerUid: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun observeHabitById(id: String): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isPaused = :isPaused, updatedAt = :timestamp WHERE id = :id")
    suspend fun setPaused(id: String, isPaused: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET isArchived = :isArchived, updatedAt = :timestamp WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM habits WHERE ownerUid = :ownerUid")
    suspend fun deleteAllForUser(ownerUid: String)
}
