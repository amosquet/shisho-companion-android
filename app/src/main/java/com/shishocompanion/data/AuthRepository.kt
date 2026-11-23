package com.shishocompanion.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.shishocompanion.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository(
    private val client: HttpClient,
    private val sharedPrefs: SharedPreferences,
    private val baseUrl: String = BuildConfig.POCKETBASE_URL,
    private val shishoApiUrl: String = BuildConfig.SHISHO_API_URL
) {
    @Inject
    constructor(
        client: HttpClient,
        sharedPrefs: SharedPreferences
    ) : this(client, sharedPrefs, BuildConfig.POCKETBASE_URL, BuildConfig.SHISHO_API_URL)
    val authToken: String?
        get() = sharedPrefs.getString("TOKEN", null)

    val userId: String?
        get() = sharedPrefs.getString("USER_ID", null)

    val userEmail: String?
        get() = sharedPrefs.getString("USER_EMAIL", null)

    suspend fun loginWithEmail(email: String, password: String): Result<AuthResponse> {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/collections/shisho_users/auth-with-password") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
            if (response.status.isSuccess()) {
                val authResponse: AuthResponse = response.body()
                sharedPrefs.edit {
                    putString("TOKEN", authResponse.token)
                    putString("USER_ID", authResponse.record.id)
                    putString("USER_EMAIL", email)
                    putBoolean("IS_GUEST", false)
                }
                Result.success(authResponse)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Login failed: ${response.status.value} - $errorBody"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<AuthResponse> {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/collections/shisho_users/records") {
                contentType(ContentType.Application.Json)
                setBody(RegisterUserRequest(email, password, password))
            }
            if (response.status.isSuccess()) {
                loginWithEmail(email, password)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Failed to register: ${response.status.value} - $errorBody"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun refreshAuthTokenIfNeeded(): Result<Boolean> {
        val token = authToken
        if (!TokenUtils.shouldRefreshToken(token)) {
            return Result.success(false)
        }
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/collections/shisho_users/auth-refresh") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val authResponse: AuthResponse = response.body()
                sharedPrefs.edit {
                    putString("TOKEN", authResponse.token)
                    putString("USER_ID", authResponse.record.id)
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Token refresh failed: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMe(): Result<UserMeResponse> {
        return try {
            val uid = userId ?: return Result.failure(Exception("Not logged in"))
            val token = authToken ?: return Result.failure(Exception("Not logged in"))
            val response: UserMeResponse = client.get("$baseUrl/api/collections/shisho_users/records/$uid") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(preferences: Preferences): Result<UserMeResponse> {
        return try {
            val uid = userId ?: return Result.failure(Exception("Not logged in"))
            val token = authToken ?: return Result.failure(Exception("Not logged in"))
            val req = UpdatePreferencesRequest(companion = preferences.companion, general = preferences.general)
            val response: UserMeResponse = client.patch("$baseUrl/api/collections/shisho_users/records/$uid") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(UpdateUserPreferencesPayload(req))
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun requestDiscordPin(discordId: String): Result<Unit> {
        return try {
            val uid = userId
            val token = authToken
            if (uid == null || token == null) {
                return Result.failure(Exception("Not logged in"))
            }
            val response: HttpResponse = client.post("$shishoApiUrl/api/auth/request-pin") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
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

    suspend fun linkDiscord(discordId: String, pin: String): Result<Unit> {
        return try {
            val uid = userId
            val token = authToken
            if (uid == null || token == null) {
                return Result.failure(Exception("Not logged in to PocketBase"))
            }
            val response: HttpResponse = client.post("$shishoApiUrl/api/auth/link-discord") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(LinkDiscordRequest(discordId = discordId, pin = pin, pbUserId = uid))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to link Discord"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun unlinkDiscord(): Result<Unit> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not logged in to PocketBase"))
            val response: HttpResponse = client.post("$shishoApiUrl/api/auth/unlink-discord") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unlink Discord"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
