package io.ontola.cache.sessions

import io.mockk.mockk
import userToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacySessionTest {
    @Test
    fun shouldHaveNoClaimsWithoutUserToken() {
        val session = LegacySession()

        assertNull(session.claims(mockk()))
    }

    @Test
    fun shouldHaveClaimsWithUserToken() {
        val session = LegacySession(userToken())
        val verifier = createJWTVerifier("12345", "CLIENT_ID")
        val claims = session.claims(verifier)

        assertNotNull(claims)
        assertEquals(claims.applicationId, "CLIENT_ID")
        assertEquals(claims.profileId, "PROFILE_ID")
        assertEquals(claims.user.email, "user@example.com")
    }

    @Test
    fun expireShouldHandleValidTokens() {
        val session = LegacySession(userToken())
        val verifier = createJWTVerifier("12345", "CLIENT_ID")

        assertFalse(session.isExpired(verifier))
    }

    @Test
    fun expireShouldHandleExpiredTokens() {
        val session = LegacySession(userToken(expired = true))
        val verifier = createJWTVerifier("12345", "CLIENT_ID")

        assertTrue(session.isExpired(verifier))
    }
}
