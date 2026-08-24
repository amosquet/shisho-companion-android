package com.shishocompanion.data

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object TokenUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrEmpty()) return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
            val jsonElement = json.parseToJsonElement(payload)
            val exp = jsonElement.jsonObject["exp"]?.jsonPrimitive?.longOrNull ?: return true
            val currentTime = System.currentTimeMillis() / 1000
            currentTime >= exp
        } catch (e: Exception) {
            true
        }
    }
}
