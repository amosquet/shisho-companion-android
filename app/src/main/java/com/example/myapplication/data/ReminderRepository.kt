package com.example.myapplication.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

import com.example.myapplication.BuildConfig

class ReminderRepository(
    private val client: HttpClient,
    private val dao: ReminderDao,
    private val baseUrl: String = BuildConfig.POCKETBASE_URL
) {
    private val collectionName = "reminders"
    var authToken: String? = null
    var userId: String? = null

    fun getReminders(): Flow<List<ReminderItem>> {
        return dao.getAllReminders().map { entities ->
            entities.map {
                ReminderItem(
                    id = it.id,
                    userId = it.userId,
                    reminderText = it.reminderText,
                    remindAt = it.remindAt,
                    isSent = it.isSent,
                    created = it.created,
                    updated = it.updated
                )
            }
        }
    }

    suspend fun sync() {
        try {
            val uid = userId ?: return
            val response: PocketBaseListResponse<ReminderItem> = 
                client.get("$baseUrl/api/collections/$collectionName/records") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                    parameter("filter", "user_id = '$uid'")
                }.body()

            dao.deleteAll()
            val entities = response.items.map {
                ReminderEntity(
                    id = it.id,
                    userId = it.userId,
                    reminderText = it.reminderText,
                    remindAt = it.remindAt,
                    isSent = it.isSent,
                    created = it.created,
                    updated = it.updated
                )
            }
            dao.insertReminders(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addReminder(req: CreateReminderRequest) {
        try {
            val remoteItem: ReminderItem = client.post("$baseUrl/api/collections/$collectionName/records") {
                authToken?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(req)
            }.body()
            sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateReminder(id: String, req: CreateReminderRequest) {
        try {
            client.patch("$baseUrl/api/collections/$collectionName/records/$id") {
                authToken?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteReminder(id: String) {
        try {
            client.delete("$baseUrl/api/collections/$collectionName/records/$id") {
                authToken?.let { header("Authorization", "Bearer $it") }
            }
            dao.deleteReminderById(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchReminderById(id: String): ReminderItem? {
        val entity = dao.getReminderById(id) ?: return null
        return ReminderItem(
            id = entity.id,
            userId = entity.userId,
            reminderText = entity.reminderText,
            remindAt = entity.remindAt,
            isSent = entity.isSent,
            created = entity.created,
            updated = entity.updated
        )
    }
}
