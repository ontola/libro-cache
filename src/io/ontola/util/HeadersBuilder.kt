package io.ontola.util

import io.ktor.http.HeadersBuilder
import io.ktor.request.ApplicationRequest
import io.ktor.request.header

fun HeadersBuilder.copy(header: String, req: ApplicationRequest) {
    req.header(header)?.let {
        set(header, it)
    }
}
