package io.ontola.util

import io.ktor.http.Url
import io.ktor.http.path
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.util.url

fun ApplicationCall.fullUrl(): Url = Url(
    url {
        path(request.path())
        parameters.appendAll(request.queryParameters)
    }
)
