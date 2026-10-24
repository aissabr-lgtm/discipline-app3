package com.discipline.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.discipline.app.data.local.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {

    @Query("SELECT * FROM reflections WHERE ownerUid = :ownerUid AND date = :date LIMIT 1")
    fun observeReflection(ownerUid: String, date: String): Flow<ReflectionEntity?>

    @Query("SELECT * FROM reflections WHERE ownerUid = :ownerUid AND date = :date LIMIT 1")
    suspend fun getReflection(ownerUid: String, date: String): ReflectionEntity?

    @Query("SELECT * FROM reflections WHERE ownerUid = :ownerUid ORDER BY date DESC")
    suspend fun getAllReflections(ownerUid: String): List<ReflectionEntity>

    @Query("SELECT * FROM reflections WHERE ownerUid = :ownerUid ORDER BY date DESC")
    fun observeAllReflections(ownerUid: String): Flow<List<ReflectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReflection(reflection: ReflectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReflections(reflections: List<ReflectionEntity>)

    @Query("DELETE FROM reflections WHERE ownerUid = :ownerUid AND date = :date")
    suspend fun deleteReflection(ownerUid: String, date: String)

    @Query("DELETE FROM reflections WHERE ownerUid = :ownerUid")
    suspend fun deleteAllForUser(ownerUid: String)
}
