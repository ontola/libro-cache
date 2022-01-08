package io.ontola.cache.sessions

import getCsrfToken
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.util.CacheHttpHeaders
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
            addManifest(Url("https://mysite.local"), Manifest.forWebsite(Url("https://mysite.local")))
        }) {
            val csrfToken = getCsrfToken()

            handleRequest(HttpMethod.Post, "/link-lib/bulk") {
                addHeader("authority", "mysite.local")
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(HttpHeaders.Accept, "application/hex+x-ndjson")
                addHeader(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                addHeader(CacheHttpHeaders.XCsrfToken, csrfToken)

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
