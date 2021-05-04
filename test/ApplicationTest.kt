package io.ontola

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ontola.cache.module

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/") {
                addHeader("authority", "localhost")
                addHeader("X-Forwarded-Proto", "https")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("UP", response.content)
            }
        }
    }
}
