package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ontola.cache.plugins.session
import io.ontola.cache.plugins.tenant

suspend fun HttpRequestBuilder.initHeaders(call: ApplicationCall, lang: String) {
    // TODO: Support direct bearer header for API requests
    val authorization = call.session.legacySession()
    val websiteIRI = call.tenant.websiteIRI
    val originalReq = call.request

    headers {
        if (!authorization?.userToken.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer ${authorization!!.userToken}")
        }

        header("Accept-Language", lang)
        header("Website-IRI", websiteIRI)

        copy("Accept-Language", originalReq)
        header("X-Forwarded-Host", originalReq.header("Host"))
        copy("X-Forwarded-Proto", originalReq)
        copy("X-Forwarded-Ssl", originalReq)
        copy("X-Request-Id", originalReq)

//                copy("Accept-Language", originalReq)
        copy("Origin", originalReq)
        copy("Referer", originalReq)
        copy("User-Agent", originalReq)
        copy("X-Forwarded-Host", originalReq)
        copy("X-Forwarded-Proto", originalReq)
        copy("X-Forwarded-Ssl", originalReq)
        copy("X-Real-Ip", originalReq)
        copy("X-Requested-With", originalReq)
        copy("X-Request-Id", originalReq)

        copy("X-Client-Ip", originalReq)
        copy("Client-Ip", originalReq)
        copy("Host", originalReq)
        copy("Forwarded", originalReq)
    }
}

fun HeadersBuilder.copy(header: String, req: ApplicationRequest) {
    req.header(header)?.let {
        set(header, it)
    }
}
