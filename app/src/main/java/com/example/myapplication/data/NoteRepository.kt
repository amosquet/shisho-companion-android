package com.example.myapplication.data

import com.example.myapplication.BuildConfig
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
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.UUID

class NoteRepository(
    private val client: HttpClient,
    private val dao: NoteDao,
    private val baseUrl: String = BuildConfig.POCKETBASE_URL
) {
    private val collectionName = "notes"
    var authToken: String? = null
    var userId: String? = null

    fun getNotes(): Flow<List<NoteItem>> {
        return dao.getAllNotes().map { entities ->
            entities.map {
                val attachmentList = try {
                    if (it.attachment.startsWith("[")) {
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(it.attachment)
                    } else if (it.attachment.isNotBlank()) {
                        listOf(it.attachment)
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    if (it.attachment.isNotBlank()) listOf(it.attachment) else emptyList()
                }

                val generatedAttachmentUrls = attachmentList.map { att ->
                    if (att.isNotEmpty() && it.id.isNotEmpty() && !it.id.startsWith("local_")) {
                        val url = if (att.startsWith("http")) att else "$baseUrl/api/files/$collectionName/${it.id}/$att"
                        if (authToken != null && !url.contains("token=")) "$url?token=$authToken" else url
                    } else if (att.startsWith("/")) {
                        "file://$att"
                    } else {
                        att
                    }
                }
                
                NoteItem(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    text = it.text,
                    attachment = generatedAttachmentUrls,
                    created = it.created,
                    updated = it.updated,
                    archived = it.archived
                )
            }
        }
    }

    suspend fun sync() {
        try {
            // 1. Upload any pending local items
            val pendingItems = dao.getPendingSyncNotes()
            for (pending in pendingItems) {
                try {
                    val attachmentList = try {
                        if (pending.attachment.startsWith("[")) {
                            kotlinx.serialization.json.Json.decodeFromString<List<String>>(pending.attachment)
                        } else if (pending.attachment.isNotBlank()) {
                            listOf(pending.attachment)
                        } else {
                            emptyList()
                        }
                    } catch (_: Exception) {
                        if (pending.attachment.isNotBlank()) listOf(pending.attachment) else emptyList()
                    }

                    client.post("$baseUrl/api/collections/$collectionName/records") {
                        authToken?.let { header("Authorization", "Bearer $it") }
                        setBody(MultiPartFormDataContent(formData {
                            append("user_id", pending.userId)
                            append("title", pending.title)
                            append("text", pending.text)
                            append("archived", pending.archived.toString())
                            if (attachmentList.isEmpty()) {
                                append("attachment", "")
                            } else {
                                attachmentList.forEach { att ->
                                    if (att.startsWith("/")) {
                                        val file = File(att)
                                        if (file.exists()) {
                                            append("attachment", file.readBytes(), Headers.build {
                                                append(HttpHeaders.ContentType, "application/octet-stream")
                                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                            })
                                        }
                                    } else {
                                        val cleanFilename = att.substringAfterLast("/").substringBefore("?")
                                        append("attachment", cleanFilename)
                                    }
                                }
                            }
                        }))
                    }

                    dao.deleteById(pending.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Fetch fresh remote list
            val response: PocketBaseListResponse<NoteItem> = 
                client.get("$baseUrl/api/collections/$collectionName/records") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                }.body()

            // 3. Sync to local database
            dao.clearSyncedNotes()
            val entities = response.items.map {
                NoteEntity(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    text = it.text,
                    attachment = kotlinx.serialization.json.Json.encodeToString(it.attachment),
                    created = it.created,
                    updated = it.updated,
                    isPendingSync = false,
                    archived = it.archived
                )
            }
            dao.insertNotes(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addNote(req: CreateNoteRequest) {
        val fakeId = "local_" + UUID.randomUUID().toString().replace("-", "")
        val newEntity = NoteEntity(
            id = fakeId,
            userId = req.userId,
            title = req.title,
            text = req.text,
            attachment = kotlinx.serialization.json.Json.encodeToString(req.attachment),
            created = "",
            updated = "",
            isPendingSync = true,
            archived = req.archived
        )
        dao.insertNote(newEntity)
        try {
            sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateNote(id: String, req: CreateNoteRequest) {
        if (id.startsWith("local_")) {
            val existing = dao.getById(id)
            if (existing != null) {
                dao.insertNote(existing.copy(
                    title = req.title, 
                    text = req.text,
                    attachment = kotlinx.serialization.json.Json.encodeToString(req.attachment),
                    archived = req.archived
                ))
            }
        } else {
            try {
                client.patch("$baseUrl/api/collections/$collectionName/records/$id") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                    setBody(MultiPartFormDataContent(formData {
                        append("user_id", req.userId)
                        append("title", req.title)
                        append("text", req.text)
                        append("archived", req.archived.toString())
                        if (req.attachment.isEmpty()) {
                            append("attachment", "")
                        } else {
                            req.attachment.forEach { att ->
                                if (att.startsWith("/")) {
                                    val file = File(att)
                                    if (file.exists()) {
                                        append("attachment", file.readBytes(), Headers.build {
                                            append(HttpHeaders.ContentType, "application/octet-stream")
                                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                        })
                                    }
                                } else {
                                    val cleanFilename = att.substringAfterLast("/").substringBefore("?")
                                    append("attachment", cleanFilename)
                                }
                            }
                        }
                    }))
                }
                sync()
            } catch (e: Exception) {
                e.printStackTrace()
                val existing = dao.getById(id)
                if (existing != null) {
                    dao.insertNote(existing.copy(
                        title = req.title, 
                        text = req.text,
                        attachment = kotlinx.serialization.json.Json.encodeToString(req.attachment),
                        archived = req.archived
                    ))
                }
            }
        }
    }

    suspend fun deleteNote(id: String) {
        if (id.startsWith("local_")) {
            dao.deleteById(id)
        } else {
            try {
                client.delete("$baseUrl/api/collections/$collectionName/records/$id") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                }
                dao.deleteById(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
