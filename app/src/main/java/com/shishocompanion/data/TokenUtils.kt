package com.shishocompanion.data

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import java.util.Date

object TokenUtils {
    /**
     * Checks if the given JWT token is expired or invalid.
     * Returns true if the token is null, blank, malformed, missing an expiration claim,
     * or past its expiration timestamp (accounting for leeway).
     */
    fun isTokenExpired(token: String?, leewaySeconds: Long = 10): Boolean {
        if (token.isNullOrBlank()) return true
        return try {
            val jwt = JWT.decode(token)
            val expiresAt = jwt.expiresAt ?: return true
            val nowWithLeeway = Date(System.currentTimeMillis() + leewaySeconds * 1000)
            expiresAt.before(nowWithLeeway)
        } catch (e: JWTDecodeException) {
            true
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Returns true if the token is valid (not yet expired) and within [refreshWindowSeconds]
     * of its expiration timestamp.
     */
    fun shouldRefreshToken(token: String?, refreshWindowSeconds: Long = 24 * 3600): Boolean {
        if (isTokenExpired(token)) return false
        return try {
            val jwt = JWT.decode(token)
            val expiresAt = jwt.expiresAt ?: return false
            val refreshThreshold = Date(System.currentTimeMillis() + refreshWindowSeconds * 1000)
            expiresAt.before(refreshThreshold)
        } catch (_: Exception) {
            false
        }
    }
}


