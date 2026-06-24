package com.shishocompanion.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shishocompanion.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID") ?: return
        val text = intent.getStringExtra("REMINDER_TEXT") ?: "You have a reminder!"

        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showNotification(context, reminderId, text)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.reminderDao()
                val reminder = dao.getReminderById(reminderId)
                if (reminder != null) {
                    dao.updateReminder(reminder.copy(isSent = true, isPendingSync = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
