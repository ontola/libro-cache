package tools.empathy.libro.server.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.testing.handleRequest
import kotlinx.coroutines.runBlocking
import tools.empathy.libro.server.sessions.SessionData
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.url.asHrefString
import withCacheTestApplication
import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class CSRFProtectionTest {
    @Test
    fun shouldGenerateTokenWithoutSession() {
        val tenant = Url("https://mysite.local")

        withCacheTestApplication({
            initTenant(tenant)
        }) { ctx ->
            handleRequest(HttpMethod.Get, "/") {
                addHeader("authority", "mysite.local")
                addHeader(HttpHeaders.Accept, "text/html")
                addHeader(HttpHeaders.XForwardedProto, "https")
                addHeader(LibroHttpHeaders.WebsiteIri, tenant.asHrefString)
            }.apply {
                val session = sessions.get<SessionData>()
                val csrfToken = session?.csrfToken
                val identity = response.cookies["libro_id"]?.value

                assertNotNull(csrfToken, "No csrf token generated")
                assertNotNull(identity, "No session set")

                runBlocking {
                    val sessionData = ctx.adapter.get("cache:session:$identity")
                    assertNotNull(sessionData)
                    assertContains(sessionData, URLEncoder.encode(csrfToken, "utf-8"))
                }

                val html = response.content
                assertNotNull(html)
                assertContains(html, "<meta name=\"csrf-token\" content=\"$csrfToken\">")
            }
        }
    }
}
