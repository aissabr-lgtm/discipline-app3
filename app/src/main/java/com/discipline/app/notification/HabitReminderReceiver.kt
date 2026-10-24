package com.discipline.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.discipline.app.MainActivity
import com.discipline.app.R
import com.discipline.app.data.local.AppDatabase
import com.discipline.app.domain.HabitScheduleEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HABIT_REMINDER = "com.discipline.app.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
        const val CHANNEL_ID = "discipline_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit reminder"

        createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val habit = db.habitDao().getHabitById(habitId)?.toDomain()
            if (habit != null) {
                val today = LocalDate.now()
                // Only notify if scheduled for today and not paused
                if (HabitScheduleEvaluator.isScheduledOnDate(habit, today)) {
                    showNotification(context, habit.id.hashCode(), habitTitle)
                }
                // Reschedule for next day
                NotificationScheduler(context).scheduleHabitReminder(habit)
            }
        }
    }

    private fun showNotification(context: Context, notificationId: Int, habitTitle: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Discipline Reminder")
            .setContentText("Time to complete: $habitTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContent)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
