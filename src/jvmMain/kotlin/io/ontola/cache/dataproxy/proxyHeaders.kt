package io.ontola.cache.dataproxy

import io.ktor.application.ApplicationCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders

internal fun HttpRequestBuilder.proxyHeaders(
    call: ApplicationCall,
    session: SessionData?,
    useWebsiteIRI: Boolean = true,
) {
    val request = call.request

    headers {
        session?.accessTokenBearer()?.let {
            header(HttpHeaders.Authorization, it)
        }
        if (useWebsiteIRI) {
            header(CacheHttpHeaders.WebsiteIri, call.tenant.websiteIRI)
        }
        proxySafeHeaders(request)
        copy(HttpHeaders.Accept, request)
        copy(HttpHeaders.ContentType, request)
        copy(HttpHeaders.Forwarded, request)
        copy(HttpHeaders.Host, request)
        copy(HttpHeaders.Origin, request)
        copy(HttpHeaders.Referrer, request)
        copy(HttpHeaders.UserAgent, request)
        copy(CacheHttpHeaders.XDeviceId, request)
        copy(CacheHttpHeaders.RequestReferrer, request)

        copy("Client-Ip", request)
        copy("X-Client-Ip", request)
        copy("X-Real-Ip", request)
        copy("X-Requested-With", request)
    }
}
