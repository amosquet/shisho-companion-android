package com.shishocompanion.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isPendingDelete = 0 ORDER BY remindAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("SELECT * FROM reminders WHERE isPendingSync = 1 AND isPendingDelete = 0")
    suspend fun getPendingSyncReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isPendingDelete = 1")
    suspend fun getPendingDeleteReminders(): List<ReminderEntity>

    @Query("UPDATE reminders SET isPendingDelete = 1, isPendingSync = 0 WHERE id = :id")
    suspend fun markPendingDelete(id: String)

    @Query("DELETE FROM reminders WHERE isPendingSync = 0 AND isPendingDelete = 0")
    suspend fun clearSyncedReminders()

    @Transaction
    suspend fun replaceSyncedReminders(reminders: List<ReminderEntity>) {
        clearSyncedReminders()
        insertReminders(reminders)
    }

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
