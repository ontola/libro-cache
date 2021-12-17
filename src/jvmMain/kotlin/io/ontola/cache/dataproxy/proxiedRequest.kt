package io.ontola.cache.dataproxy

import io.ktor.application.ApplicationCall
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenantOrNull

internal suspend fun Configuration.proxiedRequest(call: ApplicationCall, path: String, method: HttpMethod, body: Any): HttpResponse {
    if (call.tenantOrNull != null) {
        call.sessionManager.ensure()
    }

    return client.request(call.services.route(path)) {
        this.method = method
        this.body = body
        proxyHeaders(call, call.sessionManager.session, useWebsiteIRI = false)
    }
}
