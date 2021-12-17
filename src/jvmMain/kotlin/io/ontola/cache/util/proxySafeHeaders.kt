package io.ontola.cache.util

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header

fun HeadersBuilder.proxySafeHeaders(sourceRequest: ApplicationRequest, defaultLang: String? = null) {
    copy(HttpHeaders.AcceptLanguage, sourceRequest, defaultLang)
    copy(HttpHeaders.XForwardedHost, sourceRequest, sourceRequest.header(HttpHeaders.Host))
    copy(HttpHeaders.XForwardedProto, sourceRequest)
    copy("X-Forwarded-Ssl", sourceRequest)
    copy(HttpHeaders.XRequestId, sourceRequest)
}
