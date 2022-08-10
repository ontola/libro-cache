package tools.empathy.libro.server.plugins

import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import tools.empathy.libro.server.TenantNotFoundException
import tools.empathy.libro.server.WrongWebsiteIRIException
import tools.empathy.libro.server.tenantization.getWebsiteBase
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TenantizationTest {
    @Test
    fun getWebsiteBaseShouldHaveAuthoritativeHeader() {
        val headers = mapOf<String, String>()
        assertFailsWith<Exception>("No header usable for authority present") {
            getWebsiteBaseFromRequest(headers)
        }
        assertFailsWith<Exception>("No header usable for authority present") {
            getWebsiteBaseFromRequest(headers, "/path")
        }
    }

    @Test
    fun getWebsiteBaseRaisedWithoutProtocol() {
        val headers = mapOf(
            "X-Forwarded-Host" to "example.test"
        )
        assertFailsWith<Exception>("No header usable for authority present") {
            getWebsiteBaseFromRequest(headers)
        }
        assertFailsWith<Exception>("No header usable for authority present") {
            getWebsiteBaseFromRequest(headers, "/path")
        }
    }

    @Test
    fun getWebsiteBaseShouldUseWebsiteIRIHeader() {
        val headers = mapOf(
            "X-Forwarded-Host" to "example.test",
            "X-Forwarded-Proto" to "https",
            "Website-IRI" to "https://example.test/mypath"
        )

        val iri = getWebsiteBaseFromRequest(headers)
        assertEquals("https://example.test/mypath", iri)

        val iriWithPath = getWebsiteBaseFromRequest(headers, "/path")
        assertEquals("https://example.test/mypath", iriWithPath)
    }

    @Test
    fun getWebsiteBaseShouldNotBeForged() {
        val headers = mapOf(
            "X-Forwarded-Host" to "example.test",
            "X-Forwarded-Proto" to "https",
            "Website-IRI" to "https://forged.test/mypath"
        )
        assertFailsWith<WrongWebsiteIRIException> {
            getWebsiteBaseFromRequest(headers)
        }
        assertFailsWith<WrongWebsiteIRIException> {
            getWebsiteBaseFromRequest(headers, "/path")
        }
    }

    @Test
    fun getWebsiteBaseShouldBuildFromXForwardedSet() {
        val headers = mapOf(
            "X-Forwarded-Host" to "example.test",
            "X-Forwarded-Proto" to "https"
        )

        val iri = getWebsiteBaseFromRequest(headers)
        assertEquals("https://example.test/", iri)

        val iriWithPath = getWebsiteBaseFromRequest(headers, "/path")
        assertEquals("https://example.test/", iriWithPath)
    }
    @Test
    fun getWebsiteBaseShouldBuildWithPathFromXForwardedSet() {
        val headers = mapOf(
            "X-Forwarded-Host" to "exam.ple",
            "X-Forwarded-Proto" to "https"
        )

        assertFailsWith<TenantNotFoundException> {
            getWebsiteBaseFromRequest(headers)
        }

        val iriWithPath = getWebsiteBaseFromRequest(headers, "/path")
        assertEquals("https://exam.ple/path", iriWithPath)
    }

    @Test
    fun getWebsiteBaseShouldBuildIgnoreBulkRoute() {
        val headers = mapOf(
            "X-Forwarded-Host" to "example.test",
            "X-Forwarded-Proto" to "https"
        )

        val iri = getWebsiteBaseFromRequest(headers, "/link-lib/bulk")
        assertEquals("https://example.test/", iri)
    }

    @Test
    fun getWebsiteBaseShouldBuildFromOrigin() {
        val headers = mapOf(
            "Origin" to "https://example.test"
        )

        val iri = getWebsiteBaseFromRequest(headers)
        assertEquals("https://example.test/", iri)

        val iriWithPath = getWebsiteBaseFromRequest(headers, "/path")
        assertEquals("https://example.test/", iriWithPath)
    }
    @Test
    fun getWebsiteBaseWithoutSlash() {
        val headers = mapOf(
            "Origin" to "https://example.test"
        )

        val iri = getWebsiteBaseFromRequest(headers, "")
        assertEquals("https://example.test/", iri)
    }

    @Test
    fun getWebsiteBaseWithPathShouldIgnoreSlash() {
        val headers = mapOf(
            "Origin" to "https://exam.ple"
        )

        val iriWithPath = getWebsiteBaseFromRequest(headers, "/path/")
        assertEquals("https://exam.ple/path", iriWithPath)
    }

    @Test
    fun getWebsiteBaseShouldBuildWithPathFromOrigin() {
        val headers = mapOf(
            "Origin" to "https://exam.ple"
        )

        assertFailsWith<TenantNotFoundException> {
            getWebsiteBaseFromRequest(headers)
        }

        val iriWithPath = getWebsiteBaseFromRequest(headers, "/path")
        assertEquals("https://exam.ple/path", iriWithPath)
    }

    private fun getWebsiteBaseFromRequest(headers: Map<String, String>, path: String = "/"): String? {
        return withCacheTestApplication(
            {
                storage.addHashKey(LookupKeys.Manifest.name, field = "https://exam.ple/path", value = "")
                storage.addHashKey(LookupKeys.Manifest.name, field = "https://example.test/", value = "")
            }
        ) { ctx ->
            runBlocking {
                val request = createCall(
                    readResponse = true
                ) {
                    this.method = HttpMethod.Get
                    this.uri = path
                    headers.forEach { header ->
                        addHeader(header.key, header.value)
                    }
                }.request

                request.call.getWebsiteBase()
            }
        }
    }
}
