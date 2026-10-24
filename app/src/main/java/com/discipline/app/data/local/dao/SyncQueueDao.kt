package com.discipline.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.discipline.app.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE ownerUid = :ownerUid ORDER BY queueId ASC")
    suspend fun getPendingForUser(ownerUid: String): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun dequeue(queueId: Long)

    @Query("DELETE FROM sync_queue WHERE ownerUid = :ownerUid")
    suspend fun clearForUser(ownerUid: String)
}
