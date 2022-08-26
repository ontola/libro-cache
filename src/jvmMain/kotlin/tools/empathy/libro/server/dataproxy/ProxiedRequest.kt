package tools.empathy.libro.server.dataproxy

import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.tenantization.tenantOrNull

internal suspend fun Configuration.proxiedRequest(
    call: ApplicationCall,
    path: String,
    method: HttpMethod,
    body: OutgoingContent?,
): HttpResponse {
    if (call.tenantOrNull != null) {
        call.sessionManager.ensure()
    }

    val rule = matchOrDefault(path)

    val httpClient = when (rule.client) {
        ProxyClient.VerbatimBackend -> verbatimClient
        ProxyClient.RedirectingBackend -> redirectingClient
        ProxyClient.Binary -> binaryClient
    }
    val session = if (rule.includeCredentials) call.sessionManager.session else null

    return httpClient.request(call.services.route(path)) {
        this.method = method
        proxyHeaders(call, session, useWebsiteIRI = false, contentType = false)
        setBody(body)
    }
}
