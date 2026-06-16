package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    val id: String,
    val message: String,
    val timestamp: String
)
