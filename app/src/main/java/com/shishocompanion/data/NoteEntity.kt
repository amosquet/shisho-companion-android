package com.shishocompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String, // PocketBase ID, or local generated UUID if new
    val owner: String,
    val title: String,
    val text: String,
    val attachment: String, // Note: mapped from "attachment" remote field
    val created: String,
    val updated: String,
    val isPendingSync: Boolean = false,
    val archived: Boolean = false
)
