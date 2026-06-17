package com.example.myapplication.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PocketBaseListResponse<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>
)

@Serializable
data class ReadingListItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val url: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val author: String = "",
    val cover: String = "",
    val status: String = "",
    val publishDate: String = "",
    val isbn: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val collectionId: String = "",
    val collectionName: String = "",
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class CreateReadingListItemRequest(
    @SerialName("user_id") val userId: String = "",
    val title: String,
    val url: String,
    val description: String = "",
    val imageUrl: String = "",
    val author: String = "",
    val status: String = "",
    val isbn: String = "",
    val startDate: String = "",
    val endDate: String = "",
    @kotlinx.serialization.Transient var coverBytes: ByteArray? = null,
    @kotlinx.serialization.Transient var coverFilename: String? = null
)

@Serializable
data class RequestPinRequest(
    @SerialName("discord_id") val discordId: String
)

@Serializable
data class VerifyPinRequest(
    @SerialName("discord_id") val discordId: String,
    val pin: String
)

@Serializable
data class LinkDiscordRequest(
    @SerialName("discord_id") val discordId: String,
    val pin: String,
    @SerialName("pb_user_id") val pbUserId: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val record: Record
)

@Serializable
data class Record(
    val id: String
)

@Serializable
data class NoteItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val text: String = "",
    val attachment: List<String> = emptyList(), // spelling matches db
    val created: String = "",
    val updated: String = "",
    val archived: Boolean = false
)

@Serializable
data class CreateNoteRequest(
    @SerialName("user_id") val userId: String = "",
    val title: String,
    val text: String,
    val attachment: List<String> = emptyList(),
    val archived: Boolean
)

@Serializable
data class ReminderItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("reminder_text") val reminderText: String = "",
    @SerialName("remind_at") val remindAt: String = "",
    @SerialName("is_sent") val isSent: Boolean = false,
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class CreateReminderRequest(
    @SerialName("user_id") val userId: String = "",
    @SerialName("reminder_text") val reminderText: String,
    @SerialName("remind_at") val remindAt: String,
    @SerialName("is_sent") val isSent: Boolean = false
)

@Serializable
data class CompanionPreferences(
    val theme: String = "SYSTEM"
)

@Serializable
data class GeneralPreferences(
    val language: String = "en",
    @SerialName("time_format") val timeFormat: String = "SYSTEM",
    @SerialName("send_reminders_to_discord") val sendRemindersToDiscord: Boolean = false
)

@Serializable
data class Preferences(
    val companion: CompanionPreferences = CompanionPreferences(),
    val general: GeneralPreferences = GeneralPreferences()
)

@Serializable
data class UserMeResponse(
    @SerialName("discord_id") val discordId: String,
    val email: String? = null,
    val preferences: Preferences? = null
)

@Serializable
data class UpdatePreferencesRequest(
    val companion: CompanionPreferences? = null,
    val general: GeneralPreferences? = null
)

@Serializable
data class RegisterUserRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    @SerialName("discord_id") val discordId: String = "",
    val preferences: Preferences = Preferences()
)

@Serializable
data class UpdateUserPreferencesPayload(
    val preferences: UpdatePreferencesRequest
)

@Serializable
data class LoginRequest(
    val identity: String,
    val password: String
)

@Serializable
data class UnlinkDiscordRequest(
    @SerialName("pb_user_id") val pbUserId: String
)
