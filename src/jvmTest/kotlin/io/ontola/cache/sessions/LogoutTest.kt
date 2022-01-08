package io.ontola.cache.sessions

import generateTestAccessTokenPair
import getCsrfToken
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.util.CacheHttpHeaders
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class LogoutTest {
    @Test
    fun testLogout() {
        val websiteIRI = Url("https://mysite.local")
        withCacheTestApplication({
            initialAccessTokens = generateTestAccessTokenPair(false)
            addManifest(websiteIRI, Manifest.forWebsite(websiteIRI))
        }) {
            val csrfToken = getCsrfToken()

            handleRequest(HttpMethod.Post, "/logout") {
                addHeader(HttpHeaders.Origin, websiteIRI.toString())
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(CacheHttpHeaders.WebsiteIri, websiteIRI.toString())
                addHeader(CacheHttpHeaders.XCsrfToken, csrfToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
