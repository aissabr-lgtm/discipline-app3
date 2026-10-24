package com.discipline.app

import android.app.Application
import com.discipline.app.data.local.AppDatabase
import com.discipline.app.data.remote.SupabaseService
import com.discipline.app.data.remote.SyncManager
import com.discipline.app.data.repository.AuthRepository
import com.discipline.app.data.repository.ExportImportRepository
import com.discipline.app.data.repository.HabitRepository
import com.discipline.app.data.repository.PreferencesRepository
import com.discipline.app.notification.NotificationScheduler

class DisciplineApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var supabaseService: SupabaseService
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var habitRepository: HabitRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var exportImportRepository: ExportImportRepository
        private set

    lateinit var notificationScheduler: NotificationScheduler
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        supabaseService = SupabaseService(this)
        syncManager = SyncManager(database, supabaseService)
        authRepository = AuthRepository(this, supabaseService)
        habitRepository = HabitRepository(database, syncManager)
        preferencesRepository = PreferencesRepository(this)
        exportImportRepository = ExportImportRepository(database)
        notificationScheduler = NotificationScheduler(this)
    }
}
