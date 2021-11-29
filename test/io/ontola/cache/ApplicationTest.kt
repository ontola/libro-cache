package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import io.ontola.cache.plugins.SessionsConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.sessions.OIDCTokenResponse
import io.ontola.cache.sessions.UserType
import io.ontola.cache.tenantization.Manifest
import io.ontola.cache.tenantization.TenantFinderResponse
import io.ontola.cache.util.fullUrl
import io.ontola.cache.util.stem
import io.ontola.cache.util.withoutProto
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.nio.charset.Charset
import java.util.Date
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

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

        val keys = mutableListOf<String>()
        val values = mutableListOf<String>()
        coEvery { storage.set(capture(keys), capture(values)) } returns null
        val key = slot<String>()
        coEvery { storage.get(capture(key)) } answers { values[keys.indexOf(key.captured)] }

        coEvery { storage.keys("cache:routes:start:*") } returns emptyFlow()

        val setEntries = slot<Map<String, String>>()
        coEvery { storage.hset("cache:entry:https%3A//example.com/test/2:en", capture(setEntries)) } returns null

        coEvery { storage.get("cache:WebsiteBase:https%3A//mysite.local") } returns null
        coEvery { storage.get("cache:Manifest:https%3A//mysite.local") } returns Json.encodeToString(Manifest.forWebsite(Url("https://mysite.local")))
        val tenantEntries = slot<String>()
        coEvery { storage.set("cache:WebsiteBase:https%3A//mysite.local", capture(tenantEntries)) } returns null
        coEvery { storage.expire("cache:WebsiteBase:https%3A//mysite.local", 600) } returns null

        withTestApplication({ module(testing = true, storage = storage, client = client) }) {
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
                assertEquals(tenantEntries.captured, "https://mysite.local")
            }
        }
    }

    private fun createClient(resources: List<Triple<String, String, CacheControl>>): HttpClient = HttpClient(MockEngine) {
        val jsonContentType = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

        configureClient()
        engine {
            addHandler { request ->
                when (request.url.stem()) {
                    "https://data.local/_public/spi/find_tenant" -> {
                        val iri = request.url.parameters["iri"] ?: throw Exception("Tenant finder request without IRI")
                        val payload = TenantFinderResponse(
                            iriPrefix = Url(iri).withoutProto(),
                        )

                        respond(
                            Json.encodeToString(payload),
                            HttpStatusCode.OK,
                            headers = jsonContentType,
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

                        respond(
                            Json.encodeToString(payload),
                            HttpStatusCode.OK,
                            headers = jsonContentType,
                        )
                    }
                    "https://oidcserver.test/oauth/token" -> {
                        val config = SessionsConfig.forTesting()

                        val accessToken = JWT
                            .create()
                            .withClaim("application_id", config.clientId)
                            .withIssuedAt(Date.from(Clock.System.now().toJavaInstant()))
                            .withExpiresAt(Date.from(Clock.System.now().plus(1.hours).toJavaInstant()))
                            .withClaim("scopes", listOf("user"))
                            .withClaim(
                                "user",
                                mapOf(
                                    "type" to UserType.Guest.name.lowercase(),
                                    "iri" to "",
                                    "@id" to "",
                                    "id" to "",
                                    "language" to "en",
                                )
                            )
                            .withClaim("application_id", config.clientId)
                            .sign(Algorithm.HMAC512(config.jwtEncryptionToken))
                        val refreshToken = JWT
                            .create()
                            .withClaim("application_id", config.clientId)
                            .sign(Algorithm.HMAC512(config.jwtEncryptionToken))
                        val body = OIDCTokenResponse(
                            accessToken = accessToken,
                            tokenType = "",
                            expiresIn = 100,
                            refreshToken = refreshToken,
                            scope = "user",
                            createdAt = Clock.System.now().epochSeconds,
                        )

                        respond(
                            Json.encodeToString(body),
                            HttpStatusCode.OK,
                            headers = jsonContentType,
                        )
                    }
                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }
}
