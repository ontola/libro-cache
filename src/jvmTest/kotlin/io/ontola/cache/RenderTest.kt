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
import io.ontola.cache.routes.HeadResponse
import kotlinx.coroutines.runBlocking
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
                clientBuilder.setHeadResponse(Url("$websiteIRI/"), HeadResponse(HttpStatusCode.OK))
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
}
