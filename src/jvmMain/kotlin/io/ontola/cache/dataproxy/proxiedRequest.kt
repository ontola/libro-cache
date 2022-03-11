package io.ontola.cache.dataproxy

import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenantOrNull

internal suspend fun Configuration.proxiedRequest(
    call: ApplicationCall,
    path: String,
    method: HttpMethod,
    body: Any?,
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
        proxyHeaders(call, session, useWebsiteIRI = false)
        setBody(body)
    }
}
