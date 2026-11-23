package com.shishocompanion.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shishocompanion.data.AppDatabase
import com.shishocompanion.data.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("REMINDER_ID") ?: return Result.failure()
        val text = inputData.getString("REMINDER_TEXT") ?: "You have a reminder!"

        NotificationHelper.createNotificationChannel(applicationContext)
        NotificationHelper.showNotification(applicationContext, reminderId, text)

        try {
            val dao = database.reminderDao()
            val reminder = dao.getReminderById(reminderId)
            if (reminder != null) {
                dao.updateReminder(reminder.copy(isSent = true, isPendingSync = true))
                SyncManager.scheduleSync(applicationContext)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success()
    }
}
