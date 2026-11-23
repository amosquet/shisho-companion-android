package com.shishocompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.shishocompanion.MainActivity
import com.shishocompanion.R
import com.shishocompanion.data.AppDatabase
import com.shishocompanion.ui.formatReminderTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShishoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ShishoWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                updateWidgetsInternal(context, appWidgetManager, ids)
            }
        }

        private suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val (readingText, remindersText) = try {
                val db = AppDatabase.getDatabase(context)
                val books = db.readingListDao().getAllItems().first()
                    .filter { it.status.equals("reading", ignoreCase = true) }
                    .take(2)
                val reminders = db.reminderDao().getAllReminders().first()
                    .filter { !it.isSent }
                    .sortedBy { it.remindAt }
                    .take(2)

                val readingSummary = if (books.isEmpty()) {
                    context.getString(R.string.widget_no_books_reading)
                } else {
                    books.joinToString("\n") { book ->
                        if (book.totalPages > 0) {
                            "• ${book.title} (${book.currentPage}/${book.totalPages})"
                        } else {
                            "• ${book.title}"
                        }
                    }
                }

                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val remindersSummary = if (reminders.isEmpty()) {
                    context.getString(R.string.no_pending_reminders)
                } else {
                    reminders.joinToString("\n") { rem ->
                        val timeStr = if (rem.remindAt.isNotBlank()) {
                            " (${formatReminderTime(rem.remindAt, use24HourTime = is24Hour)})"
                        } else ""
                        "• ${rem.reminderText}$timeStr"
                    }
                }
                readingSummary to remindersSummary
            } catch (_: Exception) {
                context.getString(R.string.widget_no_books_reading) to context.getString(R.string.no_pending_reminders)
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_shisho)
                views.setTextViewText(R.id.widget_reading_content, readingText)
                views.setTextViewText(R.id.widget_reminders_content, remindersText)

                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    createLaunchIntent(context, null, 10)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_add_book,
                    createLaunchIntent(context, "com.shishocompanion.ADD_BOOK", 11)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_add_note,
                    createLaunchIntent(context, "com.shishocompanion.ADD_NOTE", 12)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_add_reminder,
                    createLaunchIntent(context, "com.shishocompanion.ADD_REMINDER", 13)
                )

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun createLaunchIntent(context: Context, action: String?, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                if (action != null) this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
