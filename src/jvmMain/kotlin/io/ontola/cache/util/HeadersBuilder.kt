package io.ontola.cache.util

import io.ktor.http.HeadersBuilder
import io.ktor.request.ApplicationRequest
import io.ktor.request.header

fun HeadersBuilder.copy(header: String, req: ApplicationRequest, default: String? = null) {
    (req.header(header) ?: default)?.let {
        set(header, it)
    }
}
