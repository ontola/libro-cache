package io.ontola.cache

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.formUrlEncode
import io.ktor.http.headersOf
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheEntry
import io.ontola.cache.bulk.SPIAuthorizeRequest
import io.ontola.cache.bulk.SPIResourceResponseItem
import io.ontola.cache.bulk.isA
import io.ontola.cache.bulk.statusCode
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.plugins.TenantFinderResponse
import io.ontola.cache.util.fullUrl
import io.ontola.cache.util.stem
import io.ontola.cache.util.withoutProto
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.nio.charset.Charset
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

    @Test
    fun testBulk() {
        val resource1 = "https://example.com/test/1"
        val resource2 = "https://example.com/test/2"
        val resources = listOf(
            Triple(resource1, isA(resource1, "http://schema.org/Thing"), CacheControl.Public),
            Triple(resource2, isA(resource2, "http://example.com/Thing"), CacheControl.Private),
        )
        val client = createClient(resources)

        val storage = mockk<StorageAdapter<String, String>>()
        resources.forEach { resource ->
            every { storage.hmget("cache:entry:${resource.first.replace(":", "%3A")}:en", *CacheEntry.fields) } answers {
                val data = mapOf(
                    "iri" to resource.first,
                    "status" to "200",
                    "cacheControl" to resource.third.toString(),
                    "contents" to resource.second,
                )
                val res = CacheEntry.fields.toList().map { Pair(it, data[it]) }
                flowOf(*res.toTypedArray())
            }
        }

        val setEntries = slot<Map<String, String>>()
        coEvery { storage.hset("cache:entry:https%3A//example.com/test/2:en", capture(setEntries)) } returns null

        withTestApplication({ module(testing = true, storage = storage, client = client) }) {
            handleRequest(HttpMethod.Post, "link-lib/bulk") {
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
                ).joinToString("\n")

                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(payload, response.content)
            }
        }
    }

    private fun createClient(resources: List<Triple<String, String, CacheControl>>): HttpClient = HttpClient(MockEngine) {
        configureClient()
        engine {
            addHandler { request ->
                when (request.url.stem()) {
                    "https://data.local/_public/spi/find_tenant" -> {
                        val iri = request.url.parameters["iri"] ?: throw Exception("Tenant finder request without IRI")
                        val payload = TenantFinderResponse(
                            iriPrefix = Url(iri).withoutProto(),
                        )
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            Json.encodeToString(payload),
                            HttpStatusCode.OK,
                            headers = responseHeaders,
                        )
                    }
                    "https://data.local/spi/bulk" -> {
                        val body = request.body.toByteArray().toString(Charset.defaultCharset())
                        val requestPayload = Json.decodeFromString<SPIAuthorizeRequest>(body)
                        val foundResources = resources.map { it.first }.intersect(requestPayload.resources.map { it.iri })

                        val payload = foundResources.map {
                            val (iri, payload, cacheControl) = resources.find { (key) -> key == it }!!
                            SPIResourceResponseItem(
                                iri = iri,
                                status = HttpStatusCode.OK.value,
                                cache = cacheControl,
                                language = "en",
                                body = payload,
                            )
                        }
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(
                            Json.encodeToString(payload),
                            HttpStatusCode.OK,
                            headers = responseHeaders,
                        )
                    }
                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }
}
