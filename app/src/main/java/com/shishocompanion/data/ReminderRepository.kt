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

import android.content.Context
import android.content.SharedPreferences

class ReminderRepository(
    private val client: HttpClient,
    private val dao: ReminderDao,
    private val context: Context,
    private val baseUrl: String = BuildConfig.POCKETBASE_URL,
    private val sharedPrefs: SharedPreferences? = null
) {
    private val collectionName = "reminders"
    var authToken: String? = null
        get() = field ?: sharedPrefs?.getString("TOKEN", null)
    var userId: String? = null
        get() = field ?: sharedPrefs?.getString("USER_ID", null)

    fun getReminders(): Flow<List<ReminderItem>> {
        return dao.getAllReminders().map { entities ->
            entities.map {
                ReminderItem(
                    id = it.id,
                    owner = it.owner,
                    reminderText = it.reminderText,
                    remindAt = it.remindAt,
                    isSent = it.isSent,
                    created = it.created,
                    updated = it.updated,
                    book = it.book
                )
            }
        }
    }

    suspend fun sync() {
        val token = authToken
        if (token.isNullOrEmpty() || TokenUtils.isTokenExpired(token)) return
        try {
            val pendingDeletes = dao.getPendingDeleteReminders()
            for (pending in pendingDeletes) {
                try {
                    val response = client.delete("$baseUrl/api/collections/$collectionName/records/${pending.id}") {
                        header("Authorization", "Bearer $token")
                    }
                    if (response.status.isSuccess() || response.status.value == 404) {
                        dao.deleteReminderById(pending.id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val pendingItems = dao.getPendingSyncReminders()
            for (pending in pendingItems) {
                try {
                    val req = CreateReminderRequest(
                        owner = pending.owner.ifEmpty { userId ?: "" },
                        reminderText = pending.reminderText,
                        remindAt = pending.remindAt,
                        isSent = pending.isSent,
                        book = pending.book
                    )
                    
                    val response = if (pending.id.startsWith("local_")) {
                        client.post("$baseUrl/api/collections/$collectionName/records") {
                            header("Authorization", "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(req)
                        }
                    } else {
                        client.patch("$baseUrl/api/collections/$collectionName/records/${pending.id}") {
                            header("Authorization", "Bearer $token")
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
                val allRemoteReminders = mutableListOf<ReminderItem>()
                var page = 1
                while (true) {
                    val response: PocketBaseListResponse<ReminderItem> =
                        client.get("$baseUrl/api/collections/$collectionName/records") {
                            header("Authorization", "Bearer $token")
                            parameter("filter", "owner = '$uid'")
                            parameter("page", page)
                            parameter("perPage", 500)
                        }.body()
                    allRemoteReminders.addAll(response.items)
                    if (page >= response.totalPages || response.items.isEmpty()) break
                    page++
                }

                val stillPendingIds = dao.getPendingSyncReminders().map { it.id }.toSet()
                val pendingDeleteIds = dao.getPendingDeleteReminders().map { it.id }.toSet()
                val entities = allRemoteReminders
                    .filter { it.id !in stillPendingIds && it.id !in pendingDeleteIds }
                    .map {
                        ReminderEntity(
                            id = it.id,
                            owner = it.owner,
                            reminderText = it.reminderText,
                            remindAt = it.remindAt,
                            isSent = it.isSent,
                            created = it.created,
                            updated = it.updated,
                            isPendingSync = false,
                            book = it.book
                        )
                    }
                dao.replaceSyncedReminders(entities)
                com.shishocompanion.widget.ShishoWidgetProvider.updateAllWidgets(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addReminder(req: CreateReminderRequest) {
        val fakeId = "local_" + UUID.randomUUID().toString().replace("-", "")
        val newEntity = ReminderEntity(
            id = fakeId,
            owner = req.owner.ifEmpty { userId ?: "" },
            reminderText = req.reminderText,
            remindAt = req.remindAt,
            isSent = req.isSent,
            created = "",
            updated = "",
            isPendingSync = true,
            book = req.book
        )
        dao.insertReminder(newEntity)
        com.shishocompanion.widget.ShishoWidgetProvider.updateAllWidgets(context)
        SyncManager.scheduleSync(context)
    }

    suspend fun updateReminder(id: String, req: CreateReminderRequest) {
        val existing = dao.getReminderById(id)
        if (existing != null) {
            dao.insertReminder(existing.copy(
                reminderText = req.reminderText,
                remindAt = req.remindAt,
                isSent = req.isSent,
                book = req.book,
                isPendingSync = true
            ))
            com.shishocompanion.widget.ShishoWidgetProvider.updateAllWidgets(context)
            SyncManager.scheduleSync(context)
        }
    }

    suspend fun deleteReminder(id: String) {
        if (id.startsWith("local_")) {
            dao.deleteReminderById(id)
            com.shishocompanion.widget.ShishoWidgetProvider.updateAllWidgets(context)
            return
        }
        dao.markPendingDelete(id)
        com.shishocompanion.widget.ShishoWidgetProvider.updateAllWidgets(context)
        val token = authToken
        if (!token.isNullOrEmpty() && !TokenUtils.isTokenExpired(token)) {
            try {
                val response = client.delete("$baseUrl/api/collections/$collectionName/records/$id") {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.isSuccess() || response.status.value == 404) {
                    dao.deleteReminderById(id)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        SyncManager.scheduleSync(context)
    }

    suspend fun fetchReminderById(id: String): ReminderItem? {
        val entity = dao.getReminderById(id) ?: return null
        return ReminderItem(
            id = entity.id,
            owner = entity.owner,
            reminderText = entity.reminderText,
            remindAt = entity.remindAt,
            isSent = entity.isSent,
            created = entity.created,
            updated = entity.updated,
            book = entity.book
        )
    }
}
