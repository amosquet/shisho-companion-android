package com.shishocompanion.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shishocompanion.data.AppDatabase

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("REMINDER_ID") ?: return Result.failure()
        val text = inputData.getString("REMINDER_TEXT") ?: "You have a reminder!"

        NotificationHelper.createNotificationChannel(applicationContext)
        NotificationHelper.showNotification(applicationContext, reminderId, text)

        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.reminderDao()
            val reminder = dao.getReminderById(reminderId)
            if (reminder != null) {
                dao.updateReminder(reminder.copy(isSent = true, isPendingSync = true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success()
    }
}
