package io.ontola.cache.sessions

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ontola.cache.tenantization.Manifest
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SessionRefreshTest {
    @Test
    fun testRefresh() {
        val resource1 = "https://example.com/test/1"
        val resource2 = "https://example.com/test/2"

        withCacheTestApplication({
            clientBuilder.setNewAuthorization("a", "b")
            storage.addManifest("https://mysite.local", Manifest.forWebsite(Url("https://mysite.local")))
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
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.cookies["identity"])
            }
        }
    }
}
