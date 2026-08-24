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
}


