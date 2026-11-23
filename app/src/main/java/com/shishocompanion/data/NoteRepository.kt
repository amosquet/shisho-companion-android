package com.shishocompanion.data

import com.shishocompanion.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.UUID

import android.content.Context
import android.content.SharedPreferences

class NoteRepository(
    private val client: HttpClient,
    private val dao: NoteDao,
    private val context: Context,
    private val baseUrl: String = BuildConfig.POCKETBASE_URL,
    private val sharedPrefs: SharedPreferences? = null
) {
    private val collectionName = "notes"
    var authToken: String? = null
        get() = field ?: sharedPrefs?.getString("TOKEN", null)
    var userId: String? = null
        get() = field ?: sharedPrefs?.getString("USER_ID", null)

    private fun decodeAttachments(raw: String): List<String> {
        return try {
            if (raw.startsWith("[")) {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(raw)
            } else if (raw.isNotBlank()) {
                listOf(raw)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            if (raw.isNotBlank()) listOf(raw) else emptyList()
        }
    }

    fun getNotes(): Flow<List<NoteItem>> {
        return dao.getAllNotes().map { entities ->
            entities.map {
                val attachmentList = decodeAttachments(it.attachment)

                val generatedAttachmentUrls = attachmentList.map { att ->
                    if (att.isNotEmpty() && !att.startsWith("/") && !att.startsWith("file://") && it.id.isNotEmpty() && !it.id.startsWith("local_")) {
                        if (att.startsWith("http")) att else "$baseUrl/api/files/$collectionName/${it.id}/$att"
                    } else if (att.startsWith("/")) {
                        "file://$att"
                    } else {
                        att
                    }
                }
                
                NoteItem(
                    id = it.id,
                    owner = it.owner,
                    title = it.title,
                    text = it.text,
                    attachment = generatedAttachmentUrls,
                    created = it.created,
                    updated = it.updated,
                    archived = it.archived,
                    book = it.book
                )
            }
        }
    }

    suspend fun sync() {
        val token = authToken
        if (token.isNullOrEmpty() || TokenUtils.isTokenExpired(token)) return
        try {
            // 0. Process pending deletions
            val pendingDeletes = dao.getPendingDeleteNotes()
            for (pending in pendingDeletes) {
                try {
                    val response = client.delete("$baseUrl/api/collections/$collectionName/records/${pending.id}") {
                        header("Authorization", "Bearer $token")
                    }
                    if (response.status.isSuccess() || response.status.value == 404) {
                        dao.deleteById(pending.id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 1. Upload any pending local items
            val pendingItems = dao.getPendingSyncNotes()
            for (pending in pendingItems) {
                try {
                    val attachmentList = decodeAttachments(pending.attachment)

                    val bodyContent = MultiPartFormDataContent(formData {
                        append("owner", pending.owner.ifEmpty { userId ?: "" })
                        append("title", pending.title)
                        append("text", pending.text)
                        append("archived", pending.archived.toString())
                        append("book", pending.book)
                        if (attachmentList.isEmpty()) {
                            append("attachment", "")
                        } else {
                            attachmentList.forEach { att ->
                                val localPath = if (att.startsWith("file://")) att.removePrefix("file://") else att
                                if (localPath.startsWith("/")) {
                                    val file = File(localPath)
                                    if (file.exists()) {
                                        append("attachment", file.readBytes(), Headers.build {
                                            append(HttpHeaders.ContentType, "application/octet-stream")
                                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                        })
                                    }
                                }
                            }
                        }
                    })

                    val response = if (pending.id.startsWith("local_")) {
                        client.post("$baseUrl/api/collections/$collectionName/records") {
                            header("Authorization", "Bearer $token")
                            setBody(bodyContent)
                        }
                    } else {
                        client.patch("$baseUrl/api/collections/$collectionName/records/${pending.id}") {
                            header("Authorization", "Bearer $token")
                            setBody(bodyContent)
                        }
                    }

                    if (response.status.isSuccess()) {
                        if (pending.id.startsWith("local_")) {
                            dao.deleteById(pending.id)
                        } else {
                            dao.insertNote(pending.copy(isPendingSync = false))
                        }
                    } else {
                        println("Failed to sync note: ${response.status}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Fetch fresh remote list with pagination
            val allRemoteNotes = mutableListOf<NoteItem>()
            var page = 1
            while (true) {
                val response: PocketBaseListResponse<NoteItem> =
                    client.get("$baseUrl/api/collections/$collectionName/records") {
                        header("Authorization", "Bearer $token")
                        parameter("page", page)
                        parameter("perPage", 500)
                    }.body()
                allRemoteNotes.addAll(response.items)
                if (page >= response.totalPages || response.items.isEmpty()) break
                page++
            }

            // 3. Sync to local database transactionally, preserving still-pending notes
            val stillPendingIds = dao.getPendingSyncNotes().map { it.id }.toSet()
            val pendingDeleteIds = dao.getPendingDeleteNotes().map { it.id }.toSet()
            val entities = allRemoteNotes
                .filter { it.id !in stillPendingIds && it.id !in pendingDeleteIds }
                .map {
                    NoteEntity(
                        id = it.id,
                        owner = it.owner,
                        title = it.title,
                        text = it.text,
                        attachment = kotlinx.serialization.json.Json.encodeToString(it.attachment),
                        created = it.created,
                        updated = it.updated,
                        isPendingSync = false,
                        archived = it.archived,
                        book = it.book
                    )
                }
            dao.replaceSyncedNotes(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addNote(req: CreateNoteRequest) {
        val fakeId = "local_" + UUID.randomUUID().toString().replace("-", "")
        val newEntity = NoteEntity(
            id = fakeId,
            owner = req.owner.ifEmpty { userId ?: "" },
            title = req.title,
            text = req.text,
            attachment = kotlinx.serialization.json.Json.encodeToString(req.attachment),
            created = "",
            updated = "",
            isPendingSync = true,
            archived = req.archived,
            book = req.book
        )
        dao.insertNote(newEntity)
        SyncManager.scheduleSync(context)
    }

    suspend fun updateNote(id: String, req: CreateNoteRequest) {
        val existing = dao.getById(id)
        if (id.startsWith("local_")) {
            if (existing != null) {
                dao.insertNote(existing.copy(
                    title = req.title,
                    text = req.text,
                    attachment = kotlinx.serialization.json.Json.encodeToString(req.attachment),
                    archived = req.archived,
                    book = req.book,
                    isPendingSync = true
                ))
                SyncManager.scheduleSync(context)
            }
        } else {
            val token = authToken
            try {
                if (token.isNullOrEmpty() || TokenUtils.isTokenExpired(token)) {
                    throw IllegalStateException("No valid token for immediate patch")
                }

                val existingFilenames = existing?.let { decodeAttachments(it.attachment) }
                    ?.map { it.substringAfterLast("/").substringBefore("?") }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                val keptFilenames = req.attachment
                    .filter { !it.startsWith("/") && !it.startsWith("file://") }
                    .map { it.substringAfterLast("/").substringBefore("?") }
                    .toSet()
                val removedFilenames = existingFilenames.filter { it !in keptFilenames }

                val response = client.patch("$baseUrl/api/collections/$collectionName/records/$id") {
                    header("Authorization", "Bearer $token")
                    setBody(MultiPartFormDataContent(formData {
                        append("owner", req.owner.ifEmpty { userId ?: "" })
                        append("title", req.title)
                        append("text", req.text)
                        append("archived", req.archived.toString())
                        append("book", req.book)
                        if (req.attachment.isEmpty()) {
                            append("attachment", "")
                        } else {
                            removedFilenames.forEach { removed ->
                                append("attachment-", removed)
                            }
                            req.attachment.forEach { att ->
                                val localPath = if (att.startsWith("file://")) att.removePrefix("file://") else att
                                if (localPath.startsWith("/")) {
                                    val file = File(localPath)
                                    if (file.exists()) {
                                        append("attachment", file.readBytes(), Headers.build {
                                            append(HttpHeaders.ContentType, "application/octet-stream")
                                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                        })
                                    }
                                }
                            }
                        }
                    }))
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Failed to update note: ${response.status}")
                }
                SyncManager.scheduleSync(context)
            } catch (e: Exception) {
                e.printStackTrace()
                if (existing != null) {
                    val normalizedAttachments = req.attachment.map { att ->
                        if (att.startsWith("http")) att.substringAfterLast("/").substringBefore("?")
                        else att
                    }
                    dao.insertNote(existing.copy(
                        title = req.title,
                        text = req.text,
                        attachment = kotlinx.serialization.json.Json.encodeToString(normalizedAttachments),
                        archived = req.archived,
                        book = req.book,
                        isPendingSync = true
                    ))
                    SyncManager.scheduleSync(context)
                }
            }
        }
    }

    suspend fun deleteNote(id: String) {
        if (id.startsWith("local_")) {
            dao.deleteById(id)
            return
        }
        dao.markPendingDelete(id)
        val token = authToken
        if (!token.isNullOrEmpty() && !TokenUtils.isTokenExpired(token)) {
            try {
                val response = client.delete("$baseUrl/api/collections/$collectionName/records/$id") {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.isSuccess() || response.status.value == 404) {
                    dao.deleteById(id)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        SyncManager.scheduleSync(context)
    }

}
