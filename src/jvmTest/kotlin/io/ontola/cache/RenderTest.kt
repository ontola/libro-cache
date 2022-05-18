package io.ontola.cache

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.handleRequest
import io.ontola.cache.csp.CSPValue
import io.ontola.cache.csp.cspReportEndpointPath
import io.ontola.cache.csp.nonce
import io.ontola.cache.document.seedBlock
import io.ontola.cache.routes.HeadResponse
import io.ontola.empathy.web.toSlice
import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
import it.skrape.core.htmlDocument
import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import kotlinx.serialization.json.Json
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RenderTest {
    @Test
    fun testRoot() {
        runBlocking {
            val websiteIRI = Url("https://mysite.local")

            withCacheTestApplication({
                clientBuilder.setHeadResponse(websiteIRI, HeadResponse(HttpStatusCode.OK))
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
            val data = listOf(
                Hextuple(
                    subject = "subject",
                    predicate = "predicate",
                    value = "<script src='http://test.com/script.js.jpg'</script> - <script>alert(1)</script> <base href=\"x55.is\">",
                    datatype = DataType.Literal("string"),
                    language = "",
                    graph = "http://purl.org/linked-delta/supplant",
                )
            ).toSlice()

            val html = createHTML().apply {
                body {
                    seedBlock("nonceval", data, Json)
                }
            }.finalize()

            htmlDocument(html) {
                findFirst("script#seed") {
                    assertEquals(
                        """
                        {"subject":{"_id":{"type":"id","v":"subject"},"predicate":{"type":"p","v":"&lt;script src='http://test.com/script.js.jpg'&lt;/script&gt; - &lt;script&gt;alert(1)&lt;/script&gt; &lt;base href=\"x55.is\"&gt;","dt":"string"}}}
                        """.trimIndent(),
                        this.html,
                    )
                }
            }
        }
    }
}
