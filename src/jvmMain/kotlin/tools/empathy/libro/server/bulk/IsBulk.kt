package tools.empathy.libro.server.bulk

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri

fun ApplicationCall.isBulk(): Boolean =
    request.httpMethod == HttpMethod.Post && request.uri.endsWith("/link-lib/bulk")
