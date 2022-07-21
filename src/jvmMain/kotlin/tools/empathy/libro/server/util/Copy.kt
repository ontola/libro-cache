package tools.empathy.libro.server.util

import io.ktor.http.HeadersBuilder
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header

fun HeadersBuilder.copy(header: String, req: ApplicationRequest, default: String? = null) {
    (req.header(header) ?: default)?.let {
        set(header, it)
    }
}
