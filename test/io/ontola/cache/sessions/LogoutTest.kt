package io.ontola.cache.sessions

import generateTestAccessTokenPair
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import io.ontola.cache.tenantization.Manifest
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
            handleRequest(HttpMethod.Post, "/logout") {
                addHeader(HttpHeaders.Origin, websiteIRI.toString())
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(CacheHttpHeaders.WebsiteIri, websiteIRI.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
