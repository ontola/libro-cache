package tools.empathy.libro.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import it.skrape.core.htmlDocument
import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.csp.CSPValue
import tools.empathy.libro.server.csp.cspReportEndpointPath
import tools.empathy.libro.server.csp.nonce
import tools.empathy.libro.server.document.seedBlock
import tools.empathy.libro.server.routes.HeadResponse
import tools.empathy.serialization.Value
import tools.empathy.serialization.dataSlice
import tools.empathy.serialization.field
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.url.asHref
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RenderTest {
    @Test
    fun testRoot() {
        runBlocking {
            val websiteIRI = Url("https://mysite.local/")

            withCacheTestApplication({
                clientBuilder.setHeadResponse(websiteIRI.asHref, HeadResponse(HttpStatusCode.OK))
                initTenant(websiteIRI)
            }) {
                handleRequest(HttpMethod.Get, "/") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Html.toString())
                    addHeader("authority", "mysite.local")
                    addHeader(HttpHeaders.XForwardedProto, "https")
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())

                    val cspHeader = response.headers["Content-Security-Policy"]
                    assertNotNull(cspHeader)

                    val cspComponents = cspHeader.split("; ")
                    val flags = cspComponents.first()
                    assertEquals("upgrade-insecure-requests", flags)

                    val components = cspComponents
                        .drop(1)
                        .associate {
                            val component = it.split(' ')

                            Pair(component.first(), component.drop(1))
                        }

                    assertContains(components["default-src"]!!, CSPValue.Self)
                    assertContains(components["connect-src"]!!, "https://sessions.bugsnag.com")
                    assertContains(components["script-src"]!!, CSPValue.nonce(this.nonce))
                    assertContains(components["base-uri"]!!, CSPValue.Self)
                    assertContains(components["report-uri"]!!, cspReportEndpointPath)

                    val hstsMaxAge = response
                        .headers[HttpHeaders.StrictTransportSecurity]
                        ?.split("max-age=")
                        ?.last()
                        ?.split(";")
                        ?.first()
                        ?.toLong()
                        ?: 0L
                    assertEquals(31536000L, hstsMaxAge)
                }
            }
        }
    }

    @Test
    fun testSeedEscaping() {
        runBlocking {
            val id = Value.Id.Local()
            val data = dataSlice {
                record(id) {
                    field(Value.Id.Global("pred")) {
                        s("<script src='http://test.com/script.js.jpg'</script> - <script>alert(1)</script> <base href=\"x55.is\">")
                    }
                }
            }

            val html = createHTML().apply {
                body {
                    seedBlock("nonceval", data, Json)
                }
            }.finalize()

            htmlDocument(html) {
                findFirst("script#seed") {
                    assertEquals(
                        """
                        {"${id.id}":{"_id":{"type":"lid","v":"${id.id}"},"pred":{"type":"s","v":"&lt;script src='http://test.com/script.js.jpg'&lt;/script&gt; - &lt;script&gt;alert(1)&lt;/script&gt; &lt;base href=\"x55.is\"&gt;"}}}
                        """.trimIndent(),
                        this.html,
                    )
                }
            }
        }
    }
}
