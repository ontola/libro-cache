package tools.empathy.libro.server

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.junit.Test
import tools.empathy.libro.server.bulk.CacheControl
import tools.empathy.libro.server.bulk.isA
import tools.empathy.libro.server.bulk.statusCode
import withCacheTestApplication
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testStatus() {
        withCacheTestApplication {
            handleRequest(HttpMethod.Get, "/link-lib/cache/status") {
                addHeader("authority", "mysite.local")
                addHeader("X-Forwarded-Proto", "https")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("UP", response.content)
            }
        }
    }

    @Test
    fun testBulk() {
        val resource1 = "https://example.com/test/1"
        val resource2 = "https://example.com/test/2"
        val resources = listOf(
            Triple(resource1, isA(resource1, "http://schema.org/Thing"), CacheControl.Public),
            Triple(resource2, isA(resource2, "http://example.com/Thing"), CacheControl.Private),
        )

        withCacheTestApplication({
            clientBuilder.addResources(resources)
            storage.resources.addAll(resources)

            initTenant(Url("https://mysite.local"))
        }) {
            handleRequest(HttpMethod.Post, "/link-lib/bulk") {
                addHeader("authority", "mysite.local")
                addHeader("X-Forwarded-Proto", "https")
                addHeader("Accept", "application/hex+x-ndjson")
                addHeader("Content-Type", "application/x-www-form-urlencoded")

                setBody(
                    listOf(
                        "resource[]" to resource1,
                        "resource[]" to resource2,
                    ).formUrlEncode()
                )
            }.apply {
                val payload = listOf(
                    statusCode(resource1, HttpStatusCode.OK),
                    isA(resource1, "http://schema.org/Thing"),
                    statusCode(resource2, HttpStatusCode.OK),
                    isA(resource2, "http://example.com/Thing"),
                ).joinToString("\n", postfix = "\n")

                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(payload, response.content)
            }
        }
    }
}
