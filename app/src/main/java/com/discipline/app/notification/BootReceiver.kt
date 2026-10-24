package com.discipline.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.discipline.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val scheduler = NotificationScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                // Get all unarchived and unpaused habits
                val habits = db.habitDao().getAllHabits("local_user") // plus any other active
                for (h in habits) {
                    val habit = h.toDomain()
                    if (habit.hasReminder && !habit.isPaused && !habit.isArchived) {
                        scheduler.scheduleHabitReminder(habit)
                    }
                }
            }
        }
    }
}
