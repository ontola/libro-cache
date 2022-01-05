package io.ontola.cache.plugins

import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.RequestConnectionPoint
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationReceivePipeline
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.RequestCookies
import io.ktor.server.response.ApplicationResponse
import io.ktor.util.Attributes
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.ontola.cache.tenantization.closeToWebsiteIRI
import mu.KLogger
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TenantizationTest {
    private fun createRequest(path: String, headers: Headers): ApplicationRequest {
        lateinit var request: ApplicationRequest

        val call = object : ApplicationCall {
            override val application: Application
                get() = TODO("Not yet implemented")
            override val attributes: Attributes
                get() = Attributes()
            override val parameters: Parameters
                get() = TODO("Not yet implemented")
            override val request: ApplicationRequest
                get() = request
            override val response: ApplicationResponse
                get() = TODO("Not yet implemented")

            override fun afterFinish(handler: (cause: Throwable?) -> Unit) {
                TODO("Not yet implemented")
            }
        }

        request = object : ApplicationRequest {
            override val call: ApplicationCall
                get() = call
            override val cookies: RequestCookies
                get() = TODO("Not yet implemented")
            override val headers: Headers
                get() = headers
            override val local: RequestConnectionPoint
                get() = object : RequestConnectionPoint {
                    override val host: String
                        get() = "localhost"
                    override val method: HttpMethod
                        get() = HttpMethod.Get
                    override val port: Int
                        get() = 80
                    override val remoteHost: String
                        get() = "localhost"
                    override val scheme: String
                        get() = "https"
                    override val uri: String
                        get() = path
                    override val version: String
                        get() = "1.1"
                }
            override val pipeline: ApplicationReceivePipeline
                get() = TODO("Not yet implemented")
            override val queryParameters: Parameters
                get() = TODO("Not yet implemented")
            override val rawQueryParameters: Parameters
                get() = TODO("Not yet implemented")

            override fun receiveChannel(): ByteReadChannel {
                TODO("Not yet implemented")
            }
        }

        return request
    }

    @Test
    fun closeToWebsiteIRIShouldHaveAuthoritativeHeader() {
        assertFailsWith<Exception>("No header usable for authority present") {
            createRequest("/", HeadersBuilder().build()).closeToWebsiteIRI(mockk())
        }
    }

    @Test
    fun closeToWebsiteIRIRaisedWithoutProtocol() {
        assertFailsWith<Exception>("No header usable for authority present") {
            val headers = HeadersBuilder().apply {
                append("X-Forwarded-Host", "example.test")
            }.build()
            createRequest("/", headers)
                .closeToWebsiteIRI(mockk())
        }
    }

    @Test
    fun closeToWebsiteIRIShouldUseWebsiteIRIHeader() {
        val headers = HeadersBuilder().apply {
            append("X-Forwarded-Host", "example.test")
            append("X-Forwarded-Proto", "https")
            append("Website-IRI", "https://example.test/mypath")
        }.build()
        val req = createRequest("/", headers)
        val iri = req.closeToWebsiteIRI(mockk())

        assertEquals("https://example.test/mypath", iri)
    }

    @Test
    fun closeToWebsiteIRIShouldLogForging() {
        val logger = mockk<KLogger>()
        val warning = slot<String>()
        every { logger.warn(capture(warning)) } returns Unit

        val headers = HeadersBuilder().apply {
            append("X-Forwarded-Host", "example.test")
            append("X-Forwarded-Proto", "https")
            append("Website-IRI", "https://forged.test/mypath")
        }.build()
        val req = createRequest("/", headers)
        req.closeToWebsiteIRI(logger)

        assertContains(warning.captured, "Website-Iri does not correspond")
        assertContains(warning.captured, "https://forged.test/mypath")
        assertContains(warning.captured, "https://example.test")
    }

    @Test
    fun closeToWebsiteIRIShouldBuildFromXForwardedSet() {
        val headers = HeadersBuilder().apply {
            append("X-Forwarded-Host", "example.test")
            append("X-Forwarded-Proto", "https")
        }.build()
        val req = createRequest("/", headers)
        val iri = req.closeToWebsiteIRI(mockk())

        assertEquals("https://example.test", iri)
    }

    @Test
    fun closeToWebsiteIRIShouldBuildIgnoreBulkRoute() {
        val headers = HeadersBuilder().apply {
            append("X-Forwarded-Host", "example.test")
            append("X-Forwarded-Proto", "https")
        }.build()
        val req = createRequest("/link-lib/bulk", headers)
        val iri = req.closeToWebsiteIRI(mockk())

        assertEquals("https://example.test", iri)
    }

    @Test
    fun closeToWebsiteIRIFromOrigin() {
        val headers = HeadersBuilder().apply {
            append("Origin", "https://example.test")
        }.build()
        val req = createRequest("/", headers)
        val iri = req.closeToWebsiteIRI(mockk())

        assertEquals("https://example.test", iri)
    }
}
