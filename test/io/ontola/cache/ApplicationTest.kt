package io.ontola.cache

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testStatus() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/link-lib/cache/status") {
                addHeader("authority", "mysite.local")
                addHeader("X-Forwarded-Proto", "https")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("UP", response.content)
            }
        }
    }
}
