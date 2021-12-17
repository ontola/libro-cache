package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders

fun HttpRequestBuilder.initHeaders(
    call: ApplicationCall,
    lang: String,
    websiteBase: Url = call.tenant.websiteIRI,
) {
    // TODO: Support direct bearer header for API requests
    val authorization = call.sessionManager.session
    val originalReq = call.request

    headers {
        authorization?.accessTokenBearer()?.let {
            header(HttpHeaders.Authorization, it)
        }

        header(CacheHttpHeaders.WebsiteIri, websiteBase)

        proxySafeHeaders(originalReq, lang)
        copy(HttpHeaders.Host, originalReq)
        copy(HttpHeaders.Forwarded, originalReq)
        copy(HttpHeaders.Origin, originalReq)
        copy(HttpHeaders.Referrer, originalReq)
        copy(HttpHeaders.UserAgent, originalReq)

        copy("Client-Ip", originalReq)
        copy("X-Client-Ip", originalReq)
        copy("X-Real-Ip", originalReq)
        copy("X-Requested-With", originalReq)
    }
}