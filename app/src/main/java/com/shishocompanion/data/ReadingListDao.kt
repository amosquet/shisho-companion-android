package com.shishocompanion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list WHERE isPendingDelete = 0 ORDER BY created DESC")
    fun getAllItems(): Flow<List<ReadingListEntity>>

    @Query("SELECT * FROM reading_list WHERE isPendingSync = 1 AND isPendingDelete = 0")
    suspend fun getPendingSyncItems(): List<ReadingListEntity>

    @Query("SELECT * FROM reading_list WHERE isPendingDelete = 1")
    suspend fun getPendingDeleteItems(): List<ReadingListEntity>

    @Query("UPDATE reading_list SET isPendingDelete = 1, isPendingSync = 0 WHERE id = :id")
    suspend fun markPendingDelete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReadingListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ReadingListEntity)

    @Query("DELETE FROM reading_list WHERE isPendingSync = 0 AND isPendingDelete = 0")
    suspend fun clearSyncedItems() // Helper to clear remote-sourced items if we want a fresh remote sync without deleting local unsynced

    @androidx.room.Transaction
    suspend fun replaceSyncedItems(items: List<ReadingListEntity>) {
        clearSyncedItems()
        insertItems(items)
    }

    @Query("DELETE FROM reading_list WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM reading_list WHERE id = :id")
    suspend fun getById(id: String): ReadingListEntity?
}
