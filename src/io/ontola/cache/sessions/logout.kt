package io.ontola.cache.sessions

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.plugins.tenant
import io.ontola.cache.util.proxySafeHeaders

suspend fun PipelineContext<Unit, ApplicationCall>.logout(): HttpResponse? {
    val logoutRequest = call.sessionManager.logoutRequest ?: return null

    val websiteIRI = call.tenant.websiteIRI
    val revokeUrl = websiteIRI.copy(encodedPath = websiteIRI.encodedPath + "/oauth/revoke")

    return call.tenant.client.post<HttpResponse>(call.services.route(revokeUrl.encodedPath)) {
        headers {
            if (call.sessionManager.isUser) {
                set(HttpHeaders.Authorization, call.sessionManager.session!!.accessTokenBearer())
            }
            proxySafeHeaders(call.request)
        }
        body = logoutRequest
    }
}
