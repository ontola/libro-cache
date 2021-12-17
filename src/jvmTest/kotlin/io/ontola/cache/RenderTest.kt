package io.ontola.cache

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import io.ontola.cache.routes.HeadResponse
import io.ontola.cache.tenantization.Manifest
import kotlinx.coroutines.runBlocking
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderTest {
    @Test
    fun testRoot() {
        runBlocking {
            val websiteIRI = Url("https://mysite.local")

            withCacheTestApplication({
                clientBuilder.setHeadResponse(Url("$websiteIRI/"), HeadResponse(HttpStatusCode.OK))
                addManifest(websiteIRI, Manifest.forWebsite(websiteIRI))
            }) {
                handleRequest(HttpMethod.Get, "/") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Html.toString())
                    addHeader("authority", "mysite.local")
                    addHeader(HttpHeaders.XForwardedProto, "https")
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }
        }
    }
}
