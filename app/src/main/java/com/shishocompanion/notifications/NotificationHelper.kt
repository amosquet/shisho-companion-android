package com.shishocompanion.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shishocompanion.R
import com.shishocompanion.data.ReminderItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object NotificationHelper {
    const val CHANNEL_ID = "reminders_channel"
    const val NOTIFICATION_ID_OFFSET = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminders"
            val descriptionText = "Notifications for scheduled reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleAlarm(context: Context, reminder: ReminderItem) {
        if (reminder.isSent) return

        val timeInMillis = parseTime(reminder.remindAt) ?: return
        if (timeInMillis <= System.currentTimeMillis()) return // Already passed

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TEXT", reminder.reminderText)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Exception handled silently
            // Log missing SCHEDULE_EXACT_ALARM permission
        }
    }

    fun cancelAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(context: Context, reminderId: String, text: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // fallback icon
            .setContentTitle("Reminder")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(reminderId.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Exception handled silently
            // Missing POST_NOTIFICATIONS permission
        }
    }

    private fun parseTime(timeStr: String): Long? {
        // Pocketbase format: "2022-01-01 01:02:03.456Z"
        return try {
            // Replace space with T just in case, though simple date format handles spaces directly
            val normalized = timeStr.replace("Z", "").trim()
            val format = if (normalized.contains(" ")) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            }
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(normalized)?.time
        } catch (e: Exception) {
            try {
                // Fallback for simple format
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(timeStr.replace("Z", "").trim())?.time
            } catch (e2: Exception) {
                null
            }
        }
    }
}
