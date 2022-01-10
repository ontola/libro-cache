package io.ontola.cache.dataproxy

import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.uri
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenantOrNull

internal suspend fun Configuration.proxiedRequest(call: ApplicationCall, path: String, method: HttpMethod, body: Any): HttpResponse {
    if (call.tenantOrNull != null) {
        call.sessionManager.ensure()
    }

    val httpClient = if (isBinaryRequest(Url(call.request.uri))) binaryClient else client

    return httpClient.request(call.services.route(path)) {
        this.method = method
        setBody(body)
        proxyHeaders(call, call.sessionManager.session, useWebsiteIRI = false)
    }
}
