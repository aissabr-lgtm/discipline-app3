package com.discipline.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.discipline.app.data.local.dao.HabitDao
import com.discipline.app.data.local.dao.HabitLogDao
import com.discipline.app.data.local.dao.ReflectionDao
import com.discipline.app.data.local.dao.SyncQueueDao
import com.discipline.app.data.local.entity.HabitEntity
import com.discipline.app.data.local.entity.HabitLogEntity
import com.discipline.app.data.local.entity.ReflectionEntity
import com.discipline.app.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        ReflectionEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "discipline_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
