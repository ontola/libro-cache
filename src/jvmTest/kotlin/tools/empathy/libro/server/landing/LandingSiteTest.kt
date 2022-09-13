package tools.empathy.libro.server.landing

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LandingSiteTest {
    @Test
    fun testLandingSiteRenders() {
        withCacheTestApplication {
            handleRequest(HttpMethod.Get, "/") {
                addHeader("authority", "localhost")
                addHeader(HttpHeaders.Accept, "*/*")
                addHeader(HttpHeaders.XForwardedProto, "https")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertContains(response.content ?: "", "<meta name=\"website\" content=\"https://localhost/\">")
            }
        }
    }
}
