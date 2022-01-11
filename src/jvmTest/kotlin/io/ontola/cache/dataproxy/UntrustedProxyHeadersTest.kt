package io.ontola.cache.dataproxy

import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.VaryHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UntrustedProxyHeadersTest {
    @Test
    fun shouldSetVaryHeader() {
        val response = mockk<HttpResponse>()
        every { response.headers } answers { Headers.build {} }

        val headers = untrustedProxyHeaders(
            config = Configuration(),
            isDownloadRequest = false,
            proxiedHeaders = Headers.build {},
            setAuthorization = mockk(),
            response = response,
        )

        assertEquals(headers[HttpHeaders.Vary], VaryHeader)
    }

    @Test
    fun shouldSetNewAuthorization() {
        val response = mockk<HttpResponse>()
        every { response.status } returns HttpStatusCode.OK
        every { response.headers } answers {
            Headers.build {
                append(CacheHttpHeaders.NewAuthorization, "auth")
                append(CacheHttpHeaders.NewRefreshToken, "refresh")
            }
        }
        val setAuthorization = mockk<(String, String) -> Unit>()
        every { setAuthorization(any(), any()) } returns Unit

        untrustedProxyHeaders(
            config = Configuration(),
            isDownloadRequest = false,
            proxiedHeaders = Headers.build {},
            setAuthorization = setAuthorization,
            response = response,
        )

        verify { setAuthorization wasNot Called }
    }

    @Test
    fun shouldFilterResponseHeaders() {
        val response = mockk<HttpResponse>()
        every { response.status } returns HttpStatusCode.OK
        every { response.headers } answers {
            Headers.build {
            }
        }
        val setAuthorization = mockk<(String, String) -> Unit>()
        every { setAuthorization(any(), any()) } returns Unit

        val headers = untrustedProxyHeaders(
            config = Configuration(),
            isDownloadRequest = false,
            proxiedHeaders = Headers.build {
                append(CacheHttpHeaders.ExecAction, "action")
                append(HttpHeaders.SetCookie, "chocolate")
            },
            setAuthorization = setAuthorization,
            response = response,
        )

        assertEquals(headers[CacheHttpHeaders.ExecAction], "action")
        assertNull(headers[HttpHeaders.SetCookie])
    }
}
