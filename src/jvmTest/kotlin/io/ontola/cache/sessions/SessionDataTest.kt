package io.ontola.cache.sessions

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionDataTest {
    @Test
    fun initializesWithoutCsrfToken() {
        val serialized = """{"credentials": {"accessToken": "access", "refreshToken": "refresh"}}"""

        val test = Json.decodeFromString<SessionData>(serialized)

        assert(test.csrfToken.isNotBlank())
        assertEquals("access", test.credentials!!.accessToken)
        assertEquals("refresh", test.credentials!!.refreshToken)
    }

    @Test
    fun initializesWithCsrfToken() {
        val serialized = """{"credentials": {"accessToken": "access", "refreshToken": "refresh"}, "csrfToken": "csrf"}"""

        val test = Json.decodeFromString<SessionData>(serialized)

        assertEquals("csrf", test.csrfToken)
        assertEquals("access", test.credentials!!.accessToken)
        assertEquals("refresh", test.credentials!!.refreshToken)
    }
}
