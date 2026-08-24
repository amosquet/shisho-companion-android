package com.shishocompanion.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class TokenUtilsTest {

    private fun createToken(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8))
        val payload = encoder.encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        val signature = encoder.encodeToString("fakesignature".toByteArray(Charsets.UTF_8))
        return "$header.$payload.$signature"
    }

    @Test
    fun isTokenExpired_withNullOrBlank_returnsTrue() {
        assertTrue(TokenUtils.isTokenExpired(null))
        assertTrue(TokenUtils.isTokenExpired(""))
        assertTrue(TokenUtils.isTokenExpired("   "))
    }

    @Test
    fun isTokenExpired_withMalformedToken_returnsTrue() {
        assertTrue(TokenUtils.isTokenExpired("not.a.valid.jwt"))
        assertTrue(TokenUtils.isTokenExpired("invalid_token"))
        assertTrue(TokenUtils.isTokenExpired("abc.def"))
    }

    @Test
    fun isTokenExpired_withMissingExpClaim_returnsTrue() {
        val token = createToken("""{"sub":"12345","name":"Test User"}""")
        assertTrue(TokenUtils.isTokenExpired(token))
    }

    @Test
    fun isTokenExpired_withPastExp_returnsTrue() {
        val pastTimeSeconds = (System.currentTimeMillis() / 1000) - 3600 // 1 hour ago
        val token = createToken("""{"sub":"12345","exp":$pastTimeSeconds}""")
        assertTrue(TokenUtils.isTokenExpired(token))
    }

    @Test
    fun isTokenExpired_withFutureExp_returnsFalse() {
        val futureTimeSeconds = (System.currentTimeMillis() / 1000) + 3600 // 1 hour in future
        val token = createToken("""{"sub":"12345","exp":$futureTimeSeconds}""")
        assertFalse(TokenUtils.isTokenExpired(token))
    }

    @Test
    fun isTokenExpired_withinLeeway_returnsTrue() {
        // Expired 5 seconds ago, with 0 leeway it's expired
        val past5s = (System.currentTimeMillis() / 1000) - 5
        val token = createToken("""{"sub":"12345","exp":$past5s}""")
        assertTrue(TokenUtils.isTokenExpired(token, leewaySeconds = 0))

        // Expiring 5 seconds in future, but with 10s leeway considered expired (boundary safety)
        val future5s = (System.currentTimeMillis() / 1000) + 5
        val token2 = createToken("""{"sub":"12345","exp":$future5s}""")
        assertTrue(TokenUtils.isTokenExpired(token2, leewaySeconds = 10))
    }
}
