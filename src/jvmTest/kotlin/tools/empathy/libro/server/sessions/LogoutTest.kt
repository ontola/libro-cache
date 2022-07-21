package tools.empathy.libro.server.sessions

import generateTestAccessTokenPair
import getCsrfToken
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import tools.empathy.libro.server.util.LibroHttpHeaders
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class LogoutTest {
    @Test
    fun testLogout() {
        val websiteIRI = Url("https://mysite.local")
        withCacheTestApplication({
            initialAccessTokens = generateTestAccessTokenPair(false)
            initTenant(websiteIRI)
        }) {
            val csrfToken = getCsrfToken()

            handleRequest(HttpMethod.Post, "/logout") {
                addHeader(HttpHeaders.Origin, websiteIRI.toString())
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(LibroHttpHeaders.WebsiteIri, websiteIRI.toString())
                addHeader(LibroHttpHeaders.XCsrfToken, csrfToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
