package tools.empathy.libro.server.tenantization

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTenantTest {
    @Test
    fun protoShouldDefault() {
        val headers = Headers.build {}.proto()
        assertEquals("http", headers)
    }

    @Test
    fun protoShouldUseScheme() {
        val headers = Headers.build {
            set("scheme", "https")
        }.proto()
        assertEquals("https", headers)
    }

    @Test
    fun protoShouldUseOrigin() {
        val headers = Headers.build {
            set(HttpHeaders.Origin, "https://example.com")
        }.proto()
        assertEquals("https", headers)
    }

    @Test
    fun protoShouldUseXForwardedProto() {
        val headers = Headers.build {
            set(HttpHeaders.XForwardedProto, "https, http")
        }.proto()
        assertEquals("https", headers)
    }
}
