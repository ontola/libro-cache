package tools.empathy.libro.server.bulk

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.copy
import tools.empathy.libro.server.util.proxySafeHeaders

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

        header(LibroHttpHeaders.WebsiteIri, websiteBase)

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
