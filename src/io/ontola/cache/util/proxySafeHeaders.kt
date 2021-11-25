package io.ontola.cache.util

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.request.ApplicationRequest
import io.ktor.request.header

fun HeadersBuilder.proxySafeHeaders(sourceRequest: ApplicationRequest) {
    copy(HttpHeaders.AcceptLanguage, sourceRequest)
    copy(HttpHeaders.XForwardedHost, sourceRequest, sourceRequest.header(HttpHeaders.Host))
    copy(HttpHeaders.XForwardedProto, sourceRequest)
    copy("X-Forwarded-Ssl", sourceRequest)
    copy(HttpHeaders.XRequestId, sourceRequest)
}
