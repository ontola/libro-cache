package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.request.header
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.plugins.tenant
import io.ontola.cache.util.copy

suspend fun HttpRequestBuilder.initHeaders(call: ApplicationCall, lang: String) {
    // TODO: Support direct bearer header for API requests
    val authorization = call.sessionManager.session
    val websiteIRI = call.tenant.websiteIRI
    val originalReq = call.request

    headers {
        if (!authorization?.accessToken.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer ${authorization!!.accessToken}")
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
