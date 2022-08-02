package tools.empathy.libro.server.sessions

import generateTestAccessTokenPair
import getCsrfToken
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import kotlinx.datetime.Instant
import tools.empathy.libro.server.sessions.oidc.ClientCredentials
import tools.empathy.libro.server.sessions.oidc.OIDCServerSettings
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.url.appendPath
import tools.empathy.url.asHrefString
import tools.empathy.url.origin
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
            val origin = Url(websiteIRI.origin())
            registerOIDCServerSettings(
                OIDCServerSettings(
                    Url("https://oidcserver.test"),
                    authorizeUrl = origin.appendPath("oauth", "authorize"),
                    accessTokenUrl = origin.appendPath("oauth", "tokens"),
                    credentials = ClientCredentials(
                        clientId = "libroclient",
                        clientSecret = "librosecret",
                        clientIdIssuedAt = 0,
                        clientSecretExpiresAt = Instant.DISTANT_FUTURE.epochSeconds.toInt(),
                        listOf(Url("urn:ietf:wg:oauth:2.0:oob")),
                    )
                )
            )
        }) {
            val csrfToken = getCsrfToken()

            handleRequest(HttpMethod.Post, "/logout") {
                addHeader(HttpHeaders.Origin, websiteIRI.toString().removeSuffix("/"))
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(LibroHttpHeaders.WebsiteIri, websiteIRI.asHrefString)
                addHeader(LibroHttpHeaders.XCsrfToken, csrfToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
