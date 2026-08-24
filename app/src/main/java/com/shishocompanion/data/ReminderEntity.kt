package com.shishocompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val reminderText: String,
    val remindAt: String,
    val isSent: Boolean,
    val created: String,
    val updated: String,
    val isPendingSync: Boolean = false
)
