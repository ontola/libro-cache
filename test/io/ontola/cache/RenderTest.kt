package io.ontola.cache

import TestClientBuilder
import blankStorage
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ontola.cache.plugins.Manifest
import kotlinx.coroutines.runBlocking
import setManifest
import setWebsiteBase
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderTest {
    @Test
    fun testRoot() {
        runBlocking {
            val websiteIRI = Url("https://mysite.local")

            val client = TestClientBuilder()
                .setHeadResponse(Url("$websiteIRI/"), HeadResponse(HttpStatusCode.OK))
                .build()

            val storage = blankStorage()
            storage.setWebsiteBase(Url("https://mysite.local"), websiteIRI)
            storage.setManifest(websiteIRI, Manifest.forWebsite(websiteIRI))

            withTestApplication({ module(testing = true, storage = storage, client = client) }) {
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
