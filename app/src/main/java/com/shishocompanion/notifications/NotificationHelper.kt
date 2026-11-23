package com.shishocompanion.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.shishocompanion.R
import com.shishocompanion.data.ReminderItem
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "reminders_channel"
    const val NOTIFICATION_ID_OFFSET = 1000

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName("Reminders")
            .setDescription("Notifications for scheduled reminders")
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun scheduleAlarm(context: Context, reminder: ReminderItem) {
        if (reminder.isSent) return

        val timeInMillis = parseTime(reminder.remindAt) ?: return
        val delay = timeInMillis - System.currentTimeMillis()
        if (delay <= 0) return // Already passed

        val data = Data.Builder()
            .putString("REMINDER_ID", reminder.id)
            .putString("REMINDER_TEXT", reminder.reminderText)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(reminder.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${reminder.id}",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAlarm(context: Context, reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        WorkManager.getInstance(context).cancelAllWorkByTag(reminderId)
    }

    fun cancelAllAlarms(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
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
            e.printStackTrace()
            // Missing POST_NOTIFICATIONS permission
        }
    }

    private val pocketBaseFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .appendOptional(DateTimeFormatter.ofPattern("'T'"))
        .appendOptional(DateTimeFormatter.ofPattern("' '"))
        .appendPattern("HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .toFormatter()

    private fun parseTime(timeStr: String): Long? {
        // Pocketbase format: "2022-01-01 01:02:03.456Z"
        return try {
            val normalized = timeStr.replace("Z", "").trim()
            val localDateTime = LocalDateTime.parse(normalized, pocketBaseFormatter)
            localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}
