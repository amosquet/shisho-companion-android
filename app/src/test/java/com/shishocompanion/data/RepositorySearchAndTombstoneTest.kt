package com.shishocompanion.data

import android.content.ContextWrapper
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class RepositorySearchAndTombstoneTest {

    private class FakeReadingListDao : ReadingListDao {
        private val items = MutableStateFlow<List<ReadingListEntity>>(emptyList())

        override fun getAllItems(): Flow<List<ReadingListEntity>> =
            items.map { list -> list.filter { !it.isPendingDelete } }

        override suspend fun getPendingSyncItems(): List<ReadingListEntity> =
            items.value.filter { it.isPendingSync && !it.isPendingDelete }

        override suspend fun getPendingDeleteItems(): List<ReadingListEntity> =
            items.value.filter { it.isPendingDelete }

        override suspend fun markPendingDelete(id: String) {
            items.value = items.value.map {
                if (it.id == id) it.copy(isPendingDelete = true, isPendingSync = false) else it
            }
        }

        override suspend fun insertItems(items: List<ReadingListEntity>) {
            val map = this.items.value.associateBy { it.id }.toMutableMap()
            items.forEach { map[it.id] = it }
            this.items.value = map.values.toList()
        }

        override suspend fun insertItem(item: ReadingListEntity) {
            insertItems(listOf(item))
        }

        override suspend fun clearSyncedItems() {
            items.value = items.value.filter { it.isPendingSync || it.isPendingDelete }
        }

        override suspend fun deleteById(id: String) {
            items.value = items.value.filterNot { it.id == id }
        }

        override suspend fun getById(id: String): ReadingListEntity? =
            items.value.find { it.id == id }
    }

    private fun validToken(): String =
        JWT.create()
            .withSubject("user1")
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256("secret"))

    @Test
    fun searchOpenLibrary_parsesDocsIntoVolumeInfo() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("openlibrary.org", request.url.host)
            respond(
                content = """
                    {
                      "docs": [
                        {
                          "title": "Dune",
                          "author_name": ["Frank Herbert"],
                          "first_publish_year": 1965,
                          "isbn": ["9780441172719"],
                          "cover_i": 12345,
                          "number_of_pages_median": 688
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repo = ReadingListRepository(
            client = client,
            dao = FakeReadingListDao(),
            context = ContextWrapper(null),
            baseUrl = "https://pb.example.com",
            shishoApiUrl = "https://api.example.com"
        )

        val results = repo.searchOpenLibrary(title = "Dune")
        assertEquals(1, results.size)
        assertEquals("Dune", results[0].title)
        assertEquals(listOf("Frank Herbert"), results[0].authors)
        assertEquals(688, results[0].pageCount)
        assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", results[0].imageLinks?.thumbnail)
    }

    @Test
    fun sync_processesPendingDeleteTombstonesBeforeRefreshing() = runTest {
        val dao = FakeReadingListDao()
        dao.insertItem(
            ReadingListEntity(
                id = "remote1",
                owner = "u1",
                title = "Deleted Book",
                url = "",
                description = "",
                imageUrl = "",
                author = "",
                cover = "",
                status = "read",
                publishDate = "",
                isbn = "",
                startDate = "",
                endDate = "",
                created = "",
                updated = "",
                completed = "",
                isPendingDelete = true
            )
        )

        var deleteCalled = false
        val mockEngine = MockEngine { request ->
            when {
                request.method == HttpMethod.Delete && request.url.encodedPath.endsWith("/remote1") -> {
                    deleteCalled = true
                    respond("", HttpStatusCode.NoContent)
                }
                request.method == HttpMethod.Get -> {
                    respond(
                        content = """{"page":1,"perPage":500,"totalItems":1,"totalPages":1,"items":[{"id":"remote2","title":"Kept Book","status":"reading"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repo = ReadingListRepository(
            client = client,
            dao = dao,
            context = ContextWrapper(null),
            baseUrl = "https://pb.example.com",
            shishoApiUrl = "https://api.example.com"
        ).apply {
            authToken = validToken()
            userId = "u1"
        }

        repo.sync()

        assertTrue(deleteCalled)
        assertEquals(emptyList<ReadingListEntity>(), dao.getPendingDeleteItems())
        val currentList = repo.getReadingList().first()
        assertEquals(listOf("remote2"), currentList.map { it.id })
    }
}
