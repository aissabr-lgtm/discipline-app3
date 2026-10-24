package com.discipline.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val queueId: Long = 0,
    val ownerUid: String,
    val entityType: String, // "HABIT", "LOG", "REFLECTION"
    val entityId: String,
    val action: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
