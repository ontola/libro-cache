package tools.empathy.libro.server.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RedirectTest {
    @Test
    fun shouldRedirectExact() {
        val redirects = mapOf(
            "https://example.com/" to "https://exam.ple/"
        )

        assertEquals(
            "https://exam.ple/",
            getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/")
        )

        assertNull(getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/path"))
    }

    @Test
    fun shouldRedirectPrefix() {
        val redirects = mapOf(
            "https://example.com/" to "https://exam.ple/"
        )

        assertEquals(
            "https://exam.ple/",
            getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/")
        )

        assertEquals(
            "https://exam.ple/path",
            getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/path")
        )
    }

    @Test
    fun shouldRedirectPathExact() {
        val redirects = mapOf(
            "https://example.com/path" to "https://exam.ple/"
        )

        assertEquals(
            "https://exam.ple/",
            getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/path")
        )
        assertNull(getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/path/nested"))
        assertNull(getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/"))
    }

    @Test
    fun shouldRedirectPathPrefix() {
        val redirects = mapOf(
            "https://example.com/path" to "https://exam.ple/"
        )

        assertEquals(
            "https://exam.ple/",
            getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/path")
        )

        assertEquals(
            "https://exam.ple/nested",
            getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/path/nested")
        )
        assertNull(getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/"))
    }

    @Test
    fun shouldRedirectExactToPath() {
        val redirects = mapOf(
            "https://example.com/" to "https://exam.ple/path"
        )

        assertEquals(
            "https://exam.ple/path",
            getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/")
        )

        assertNull(getRedirectForRequest(redirects, RedirectKeys.Exact, "example.com", "/path"))
    }

    @Test
    fun shouldRedirectPrefixToPath() {
        val redirects = mapOf(
            "https://example.com/" to "https://exam.ple/path"
        )

        assertEquals(
            "https://exam.ple/path",
            getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/")
        )

        assertEquals(
            "https://exam.ple/path/nested",
            getRedirectForRequest(redirects, RedirectKeys.Prefix, "example.com", "/nested")
        )
    }

    private fun getRedirectForRequest(redirects: Map<String, String>, type: RedirectKeys, host: String, path: String = "/"): String? {
        val websiteIRI = Url("https://$host")

        return withCacheTestApplication(
            {
                redirects.forEach {
                    storage.addHashKey(LookupKeys.Redirect.name, type.name, field = it.key, value = it.value)
                }
                initTenant(websiteIRI)
            }
        ) { ctx ->
            val request = handleRequest(HttpMethod.Get, path) {
                addHeader("authority", host)
                addHeader(HttpHeaders.Host, host)
                addHeader(HttpHeaders.XForwardedProto, "https")
            }

            request.response.headers[HttpHeaders.Location]
        }
    }
}
