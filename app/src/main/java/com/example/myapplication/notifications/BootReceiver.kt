package com.example.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" || 
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            val db = AppDatabase.getDatabase(context)
            val dao = db.reminderDao()
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminders = dao.getAllReminders().first()
                    reminders.forEach { entity ->
                        if (!entity.isSent) {
                            val item = com.example.myapplication.data.ReminderItem(
                                id = entity.id,
                                userId = entity.userId,
                                reminderText = entity.reminderText,
                                remindAt = entity.remindAt,
                                isSent = entity.isSent,
                                created = entity.created,
                                updated = entity.updated
                            )
                            NotificationHelper.scheduleAlarm(context, item)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
