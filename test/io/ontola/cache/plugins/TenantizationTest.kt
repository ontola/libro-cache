package io.ontola.cache.plugins

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.request.path
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.ontola.cache.module
import io.ontola.cache.tenantization.closeToWebsiteIRI
import mu.KLogger
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TenantizationTest {
    @Test
    fun testKtorPathStartsWithSlash() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/") {
                addHeader(HttpHeaders.Origin, "https://mysite.local")
            }.apply {
                assertEquals("/", this.request.path())
            }
        }
    }

    @Test
    fun closeToWebsiteIRIShouldHaveAuthoritativeHeader() {
        assertFailsWith<Exception>("No header usable for authority present") {
            closeToWebsiteIRI("/", HeadersBuilder().build(), mockk())
        }
    }

    @Test
    fun closeToWebsiteIRIRaisedWithoutProtocol() {
        assertFailsWith<Exception>("No header usable for authority present") {
            val headers = HeadersBuilder().apply {
                append("X-Forwarded-Host", "example.test")
            }.build()
            closeToWebsiteIRI("/", headers, mockk())
        }
    }

    @Test
    fun closeToWebsiteIRIShouldUseWebsiteIRIHeader() {
        val headers = HeadersBuilder().apply {
            append("X-Forwarded-Host", "example.test")
            append("X-Forwarded-Proto", "https")
            append("Website-IRI", "https://example.test/mypath")
        }.build()
        val iri = closeToWebsiteIRI("/", headers, mockk())

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
        closeToWebsiteIRI("/", headers, logger)

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
        val iri = closeToWebsiteIRI("/", headers, mockk())

        assertEquals("https://example.test", iri)
    }

    @Test
    fun closeToWebsiteIRIShouldBuildIgnoreBulkRoute() {
        val headers = HeadersBuilder().apply {
            append("X-Forwarded-Host", "example.test")
            append("X-Forwarded-Proto", "https")
        }.build()
        val iri = closeToWebsiteIRI("/link-lib/bulk", headers, mockk())

        assertEquals("https://example.test", iri)
    }

    @Test
    fun closeToWebsiteIRIFromOrigin() {
        val headers = HeadersBuilder().apply {
            append("Origin", "https://example.test")
        }.build()
        val iri = closeToWebsiteIRI("/", headers, mockk())

        assertEquals("https://example.test", iri)
    }
}
