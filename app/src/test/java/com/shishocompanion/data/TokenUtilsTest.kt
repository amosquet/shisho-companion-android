package com.shishocompanion.data

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TokenUtilsTest {

    private fun createJwtWithExpiryOffset(offsetMillis: Long): String {
        return JWT.create()
            .withSubject("user123")
            .withExpiresAt(Date(System.currentTimeMillis() + offsetMillis))
            .sign(Algorithm.HMAC256("test-secret"))
    }

    @Test
    fun isTokenExpired_handlesNullMalformedExpiredAndValid() {
        assertTrue(TokenUtils.isTokenExpired(null))
        assertTrue(TokenUtils.isTokenExpired(""))
        assertTrue(TokenUtils.isTokenExpired("not-a-jwt"))
        assertTrue(TokenUtils.isTokenExpired(createJwtWithExpiryOffset(-60_000)))
        assertFalse(TokenUtils.isTokenExpired(createJwtWithExpiryOffset(3_600_000)))
    }

    @Test
    fun shouldRefreshToken_returnsTrueOnlyWithinRefreshWindow() {
        val expiringSoon = createJwtWithExpiryOffset(30 * 60_000L) // 30 mins left (< 24h)
        val expiringLater = createJwtWithExpiryOffset(72 * 3_600_000L) // 72h left (> 24h)
        val alreadyExpired = createJwtWithExpiryOffset(-10_000L)

        assertTrue(TokenUtils.shouldRefreshToken(expiringSoon))
        assertFalse(TokenUtils.shouldRefreshToken(expiringLater))
        assertFalse(TokenUtils.shouldRefreshToken(alreadyExpired))
        assertFalse(TokenUtils.shouldRefreshToken(null))
    }
}
