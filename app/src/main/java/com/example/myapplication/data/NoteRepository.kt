package com.example.myapplication.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import io.ktor.client.request.forms.*
import java.io.File

import com.example.myapplication.BuildConfig

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
                val generatedAttachmentUrl = if (it.attachment.isNotEmpty() && it.id.isNotEmpty() && !it.id.startsWith("local_")) {
                    val url = if (it.attachment.startsWith("http")) it.attachment else "$baseUrl/api/files/$collectionName/${it.id}/${it.attachment}"
                    if (authToken != null && !url.contains("token=")) "$url?token=$authToken" else url
                } else if (it.attachment.startsWith("/")) {
                    "file://${it.attachment}"
                } else {
                    it.attachment
                }
                
                NoteItem(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    text = it.text,
                    attachment = generatedAttachmentUrl,
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
                val createReq = CreateNoteRequest(
                    userId = userId ?: "",
                    title = pending.title,
                    text = pending.text,
                    attachment = pending.attachment,
                    archived = pending.archived
                )
                try {
                    val remoteItem: NoteItem = client.post("$baseUrl/api/collections/$collectionName/records") {
                        authToken?.let { header("Authorization", "Bearer $it") }
                        if (createReq.attachment.isNotEmpty() && createReq.attachment.startsWith("/")) {
                            val file = File(createReq.attachment)
                            if (file.exists()) {
                                setBody(MultiPartFormDataContent(formData {
                                    append("user_id", createReq.userId)
                                    append("title", createReq.title)
                                    append("text", createReq.text)
                                    append("archived", createReq.archived.toString())
                                    append("attachment", file.readBytes(), Headers.build {
                                        append(HttpHeaders.ContentType, "application/octet-stream")
                                        append(HttpHeaders.ContentDisposition, "form-data; name=\"attachment\"; filename=\"${file.name}\"")
                                    })
                                }))
                            } else {
                                contentType(ContentType.Application.Json)
                                setBody(createReq)
                            }
                        } else {
                            contentType(ContentType.Application.Json)
                            setBody(createReq)
                        }
                    }.body()
                    
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
                    attachment = it.attachment,
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
            attachment = req.attachment,
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
                    attachment = req.attachment,
                    archived = req.archived
                ))
            }
        } else {
            try {
                client.patch("$baseUrl/api/collections/$collectionName/records/$id") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                    if (req.attachment.isNotEmpty() && req.attachment.startsWith("/")) {
                        val file = File(req.attachment)
                        if (file.exists()) {
                            setBody(MultiPartFormDataContent(formData {
                                append("user_id", req.userId)
                                append("title", req.title)
                                append("text", req.text)
                                append("archived", req.archived.toString())
                                append("attachment", file.readBytes(), Headers.build {
                                    append(HttpHeaders.ContentType, "application/octet-stream")
                                    append(HttpHeaders.ContentDisposition, "form-data; name=\"attachment\"; filename=\"${file.name}\"")
                                })
                            }))
                        } else {
                            contentType(ContentType.Application.Json)
                            setBody(req.copy(attachment = "")) // Send empty if not a file to avoid error or ignore
                        }
                    } else {
                        // PocketBase expects file fields to be files or empty. Passing a URL string might fail.
                        // For updates, we can just omit it by using MultiPart if we don't want to change it.
                        // Or we can just send JSON with attachment = ""
                        contentType(ContentType.Application.Json)
                        setBody(req.copy(attachment = "")) 
                    }
                }
                sync()
            } catch (e: Exception) {
                e.printStackTrace()
                val existing = dao.getById(id)
                if (existing != null) {
                    dao.insertNote(existing.copy(
                        title = req.title, 
                        text = req.text,
                        attachment = req.attachment,
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
    
    fun getNoteById(id: String): NoteItem? {
        // This is a simplistic implementation. For real use, we might need a suspend function hitting DB directly.
        // We will just read from the flow or provide a suspend version if needed.
        return null // Will implement a proper suspend fetch if needed, but UI usually has the item in the list
    }
    
    suspend fun fetchNoteById(id: String): NoteItem? {
        val entity = dao.getById(id) ?: return null
        val generatedAttachmentUrl = if (entity.attachment.isNotEmpty() && entity.id.isNotEmpty() && !entity.id.startsWith("local_")) {
            val url = if (entity.attachment.startsWith("http")) entity.attachment else "$baseUrl/api/files/$collectionName/${entity.id}/${entity.attachment}"
            if (authToken != null && !url.contains("token=")) "$url?token=$authToken" else url
        } else if (entity.attachment.startsWith("/")) {
            "file://${entity.attachment}"
        } else {
            entity.attachment
        }
        return NoteItem(
            id = entity.id,
            userId = entity.userId,
            title = entity.title,
            text = entity.text,
            attachment = generatedAttachmentUrl,
            created = entity.created,
            updated = entity.updated,
            archived = entity.archived
        )
    }
}
