package io.ontola.cache.sessions

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.ontola.cache.features.LibroSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import userToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionTest {
    private val validSessionKeys = Triple(
        "1234567890",
        "f62ec259-8120-404d-b286-47e26eefaae4",
        "efeLEKYXFLzm2lIojWQpk682rZs",
    )

    @Test
    fun shouldNotProcessWithoutSessionId() {
        val session = Session(mockk(), mockk())
        runBlocking {
            val legacy = session.legacySession()

            assertNull(legacy)
        }
    }

    @Test
    fun shouldNotReprocessSession() {
        val existing = mockk<LegacySession>()
        val session = Session(mockk(), mockk(), session = existing)
        runBlocking {
            assertEquals(session.legacySession(), existing)
        }
    }

    @Test
    fun shouldVerifySignature() {
        val valid = verifySignature(
            "koa:sess",
            validSessionKeys.first,
            validSessionKeys.second,
            validSessionKeys.third
        )

        assertTrue(valid)
    }

    @Test
    fun shouldNotVerifyInvalidSignature() {
        val valid = verifySignature(
            "koa:sess",
            validSessionKeys.first,
            validSessionKeys.second,
            "${validSessionKeys.third}a"
        )

        assertFalse(valid)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Test
    fun shouldRefreshExpiredSession() {
        val (sessionSecret, sessionId, sessionSig) = validSessionKeys

        val conf = mockk<LibroSession.Configuration>(relaxed = true)
        every { conf.cookieNameLegacy } returns "koa:sess"
        every { conf.sessionSecret } returns sessionSecret
        every { conf.jwtValidator } returns createJWTVerifier("12345", "CLIENT_ID")

        val client = mockk<RedisCoroutinesCommands<String, String>>()
        val storedSession = LegacySession(userToken = userToken(true))
        coEvery { client.get(sessionId) } returns Json.encodeToString(storedSession)
        every { conf.libroRedisConn } returns client

        val refresher = mockk<SessionRefresher>(relaxed = true)
        val refreshed = mockk<LegacySession>(relaxed = true)
        every { refreshed.userToken } returns "token"

        val session = Session(conf, refresher, sessionId, sessionSig, session = null)
        coEvery { refresher.refresh(sessionId, any()) } answers { refreshed }

        runBlocking {
            val legacy = session.legacySession()

            assertEquals("token", legacy?.userToken)
            assertEquals(refreshed, legacy)
        }
    }
}
