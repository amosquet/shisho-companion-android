package com.shishocompanion.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

import com.shishocompanion.BuildConfig

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
        if (authToken.isNullOrEmpty()) return
        if (TokenUtils.isTokenExpired(authToken)) return
        try {
            val pendingItems = dao.getPendingSyncReminders()
            for (pending in pendingItems) {
                try {
                    val req = CreateReminderRequest(
                        userId = pending.userId,
                        reminderText = pending.reminderText,
                        remindAt = pending.remindAt,
                        isSent = pending.isSent
                    )
                    
                    val response = if (pending.id.startsWith("local_")) {
                        client.post("$baseUrl/api/collections/$collectionName/records") {
                            authToken?.let { header("Authorization", "Bearer $it") }
                            contentType(ContentType.Application.Json)
                            setBody(req)
                        }
                    } else {
                        client.patch("$baseUrl/api/collections/$collectionName/records/${pending.id}") {
                            authToken?.let { header("Authorization", "Bearer $it") }
                            contentType(ContentType.Application.Json)
                            setBody(req)
                        }
                    }
                    
                    if (response.status.isSuccess()) {
                        if (pending.id.startsWith("local_")) {
                            dao.deleteReminderById(pending.id)
                        } else {
                            dao.updateReminder(pending.copy(isPendingSync = false))
                        }
                    } else {
                        println("Failed to sync reminder: ${response.status}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val uid = userId
            if (!uid.isNullOrEmpty()) {
                val response: PocketBaseListResponse<ReminderItem> = 
                    client.get("$baseUrl/api/collections/$collectionName/records") {
                        authToken?.let { header("Authorization", "Bearer $it") }
                        parameter("filter", "user_id = '$uid'")
                        parameter("perPage", 500)
                    }.body()

                dao.clearSyncedReminders()
                val entities = response.items.map {
                    ReminderEntity(
                        id = it.id,
                        userId = it.userId,
                        reminderText = it.reminderText,
                        remindAt = it.remindAt,
                        isSent = it.isSent,
                        created = it.created,
                        updated = it.updated,
                        isPendingSync = false
                    )
                }
                dao.insertReminders(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addReminder(req: CreateReminderRequest) {
        val fakeId = "local_" + UUID.randomUUID().toString().replace("-", "")
        val newEntity = ReminderEntity(
            id = fakeId,
            userId = req.userId,
            reminderText = req.reminderText,
            remindAt = req.remindAt,
            isSent = req.isSent,
            created = "",
            updated = "",
            isPendingSync = true
        )
        dao.insertReminder(newEntity)
        try {
            sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateReminder(id: String, req: CreateReminderRequest) {
        val existing = dao.getReminderById(id)
        if (existing != null) {
            dao.insertReminder(existing.copy(
                reminderText = req.reminderText,
                remindAt = req.remindAt,
                isSent = req.isSent,
                isPendingSync = true
            ))
            try {
                sync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteReminder(id: String) {
        if (id.startsWith("local_")) {
            dao.deleteReminderById(id)
        } else {
            try {
                client.delete("$baseUrl/api/collections/$collectionName/records/$id") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                }
                dao.deleteReminderById(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
