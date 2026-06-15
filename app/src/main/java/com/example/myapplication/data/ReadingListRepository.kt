package com.example.myapplication.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

import com.example.myapplication.BuildConfig

class ReadingListRepository(
    private val client: HttpClient,
    private val dao: ReadingListDao,
    private val baseUrl: String = BuildConfig.POCKETBASE_URL
) {
    private val collectionName = "books"
    var authToken: String? = null
    var userId: String? = null

    suspend fun requestPin(discordId: String): Result<Unit> {
        return try {
            val response: io.ktor.client.statement.HttpResponse = client.post("$baseUrl/api/auth/request-pin") {
                contentType(ContentType.Application.Json)
                setBody(RequestPinRequest(discordId))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to request PIN"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun verifyPin(discordId: String, pin: String): Result<Unit> {
        return try {
            val response: AuthResponse = client.post("$baseUrl/api/auth/verify-pin") {
                contentType(ContentType.Application.Json)
                setBody(VerifyPinRequest(discordId, pin))
            }.body()
            authToken = response.token
            userId = response.record.id
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getReadingList(): Flow<List<ReadingListItem>> {
        return dao.getAllItems().map { entities ->
            entities.map {
                val generatedImageUrl = if (it.cover.isNotEmpty() && it.id.isNotEmpty() && !it.id.startsWith("local_")) {
                    "$baseUrl/api/files/$collectionName/${it.id}/${it.cover}"
                } else {
                    it.imageUrl
                }
                
                ReadingListItem(
                    id = it.id,
                    title = it.title,
                    url = it.url,
                    description = it.description,
                    imageUrl = generatedImageUrl,
                    author = it.author,
                    cover = it.cover,
                    status = it.status,
                    publishDate = it.publishDate,
                    isbn = it.isbn,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    created = it.created,
                    updated = it.updated
                )
            }
        }
    }

    suspend fun sync() {
        try {
            // 1. Upload any pending local items
            val pendingItems = dao.getPendingSyncItems()
            for (pending in pendingItems) {
                val createReq = CreateReadingListItemRequest(
                    title = pending.title,
                    url = pending.url,
                    description = pending.description,
                    imageUrl = pending.imageUrl,
                    author = pending.author,
                    status = pending.status,
                    isbn = pending.isbn,
                    startDate = pending.startDate,
                    endDate = pending.endDate
                )
                try {
                    val remoteItem: ReadingListItem = client.post("$baseUrl/api/collections/$collectionName/records") {
                        contentType(ContentType.Application.Json)
                        authToken?.let { header("Authorization", "Bearer $it") }
                        setBody(createReq)
                    }.body()
                    
                    // Delete the local stub, we will fetch the true remote version in step 2
                    dao.deleteById(pending.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If upload fails, keep it pending
                }
            }

            // 2. Fetch fresh remote list
            val response: PocketBaseListResponse<ReadingListItem> = 
                client.get("$baseUrl/api/collections/$collectionName/records") {
                    authToken?.let { header("Authorization", "Bearer $it") }
                }.body()

            // 3. Sync to local database
            // We do strictly authoritative: delete all non-pending items locally, and insert what remote gave us.
            dao.clearSyncedItems()
            val entities = response.items.map {
                ReadingListEntity(
                    id = it.id,
                    title = it.title,
                    url = it.url,
                    description = it.description,
                    imageUrl = it.imageUrl,
                    author = it.author,
                    cover = it.cover,
                    status = it.status,
                    publishDate = it.publishDate,
                    isbn = it.isbn,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    created = it.created,
                    updated = it.updated,
                    isPendingSync = false
                )
            }
            dao.insertItems(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addItem(item: CreateReadingListItemRequest) {
        val fakeId = "local_" + UUID.randomUUID().toString().replace("-", "")
        val newEntity = ReadingListEntity(
            id = fakeId,
            title = item.title,
            url = item.url,
            description = item.description,
            imageUrl = item.imageUrl,
            author = item.author,
            cover = "",
            status = item.status,
            publishDate = "",
            isbn = item.isbn,
            startDate = item.startDate,
            endDate = item.endDate,
            created = "",
            updated = "",
            isPendingSync = true
        )
        dao.insertItem(newEntity)
        // Attempt a sync in background
        try {
            sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateItem(id: String, req: CreateReadingListItemRequest) {
        if (id.startsWith("local_")) {
            val existing = dao.getById(id)
            if (existing != null) {
                dao.insertItem(existing.copy(
                    title = req.title, 
                    url = req.url, 
                    description = req.description,
                    author = req.author,
                    status = req.status,
                    isbn = req.isbn,
                    startDate = req.startDate,
                    endDate = req.endDate
                ))
            }
        } else {
            try {
                client.patch("$baseUrl/api/collections/$collectionName/records/$id") {
                    contentType(ContentType.Application.Json)
                    authToken?.let { header("Authorization", "Bearer $it") }
                    setBody(req)
                }
                sync()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to update locally if offline
                val existing = dao.getById(id)
                if (existing != null) {
                    dao.insertItem(existing.copy(
                        title = req.title, 
                        url = req.url, 
                        description = req.description,
                        author = req.author,
                        status = req.status,
                        isbn = req.isbn,
                        startDate = req.startDate,
                        endDate = req.endDate
                    ))
                }
            }
        }
    }

    suspend fun getMe(): Result<UserMeResponse> {
        return try {
            val response: UserMeResponse = client.get("$baseUrl/api/users/me") {
                authToken?.let { header("Authorization", "Bearer $it") }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(preferences: Preferences): Result<UserMeResponse> {
        return try {
            val req = UpdatePreferencesRequest(companion = preferences.companion, general = preferences.general)
            val response: UserMeResponse = client.patch("$baseUrl/api/users/me/preferences") {
                contentType(ContentType.Application.Json)
                authToken?.let { header("Authorization", "Bearer $it") }
                setBody(req)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
