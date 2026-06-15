package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY created DESC")
    fun getAllItems(): Flow<List<ReadingListEntity>>

    @Query("SELECT * FROM reading_list WHERE isPendingSync = 1")
    suspend fun getPendingSyncItems(): List<ReadingListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReadingListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ReadingListEntity)

    @Query("DELETE FROM reading_list WHERE isPendingSync = 0")
    suspend fun clearSyncedItems() // Helper to clear remote-sourced items if we want a fresh remote sync without deleting local unsynced

    @Query("DELETE FROM reading_list WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM reading_list WHERE id = :id")
    suspend fun getById(id: String): ReadingListEntity?
}
