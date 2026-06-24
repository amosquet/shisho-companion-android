package com.shishocompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_list")
data class ReadingListEntity(
    @PrimaryKey
    val id: String, // PocketBase ID, or local generated UUID if new
    val userId: String,
    val title: String,
    val url: String,
    val description: String,
    val imageUrl: String,
    val author: String,
    val cover: String,
    val status: String,
    val publishDate: String,
    val isbn: String,
    val startDate: String,
    val endDate: String,
    val created: String,
    val updated: String,
    val isPendingSync: Boolean = false // True if added locally and needs upload
)
