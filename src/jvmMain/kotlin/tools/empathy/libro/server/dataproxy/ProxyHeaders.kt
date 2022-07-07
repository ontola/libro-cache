package tools.empathy.libro.server.dataproxy

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.plugins.deviceId
import tools.empathy.libro.server.sessions.SessionData
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.copy
import tools.empathy.libro.server.util.proxySafeHeaders

internal fun HttpRequestBuilder.proxyHeaders(
    call: ApplicationCall,
    session: SessionData?,
    useWebsiteIRI: Boolean = true,
    contentType: Boolean = true,
) {
    val request = call.request

    headers {
        if (request.headers.contains(HttpHeaders.Authorization)) {
            copy(HttpHeaders.Authorization, request)
        } else {
            session?.accessTokenBearer()?.let {
                header(HttpHeaders.Authorization, it)
            }
        }
        if (useWebsiteIRI) {
            header(LibroHttpHeaders.WebsiteIri, call.tenant.websiteIRI)
        }
        proxySafeHeaders(request)
        copy(HttpHeaders.Accept, request)
        if (contentType) {
            copy(HttpHeaders.ContentType, request)
        }
        copy(HttpHeaders.Forwarded, request)
        copy(HttpHeaders.Origin, request)
        copy(HttpHeaders.Referrer, request)
        copy(HttpHeaders.UserAgent, request)
        copy(HttpHeaders.AccessControlRequestHeaders, request)
        copy(HttpHeaders.AccessControlRequestMethod, request)
        header(LibroHttpHeaders.XDeviceId, call.deviceId)
        copy(LibroHttpHeaders.RequestReferrer, request)

        copy("Client-Ip", request)
        copy("X-Client-Ip", request)
        copy("X-Real-Ip", request)
        copy("X-Requested-With", request)
    }
}
