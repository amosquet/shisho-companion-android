package com.shishocompanion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isPendingDelete = 0 ORDER BY CASE WHEN updated != '' AND updated != created THEN 0 ELSE 1 END ASC, updated DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isPendingSync = 1 AND isPendingDelete = 0")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isPendingDelete = 1")
    suspend fun getPendingDeleteNotes(): List<NoteEntity>

    @Query("UPDATE notes SET isPendingDelete = 1, isPendingSync = 0 WHERE id = :id")
    suspend fun markPendingDelete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notes WHERE isPendingSync = 0 AND isPendingDelete = 0")
    suspend fun clearSyncedNotes()

    @androidx.room.Transaction
    suspend fun replaceSyncedNotes(notes: List<NoteEntity>) {
        clearSyncedNotes()
        insertNotes(notes)
    }
}
