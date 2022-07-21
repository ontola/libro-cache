package tools.empathy.libro.server.util

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header

/**
 * Copy headers considered safe to proxy from [sourceRequest] to [HeadersBuilder].
 */
fun HeadersBuilder.proxySafeHeaders(sourceRequest: ApplicationRequest, defaultLang: String? = null) {
    copy(HttpHeaders.AcceptLanguage, sourceRequest, defaultLang)
    copy(HttpHeaders.XForwardedHost, sourceRequest, sourceRequest.header(HttpHeaders.Host))
    copy(HttpHeaders.XForwardedProto, sourceRequest)
    copy("X-Forwarded-Ssl", sourceRequest)
    copy(HttpHeaders.XRequestId, sourceRequest)
}
