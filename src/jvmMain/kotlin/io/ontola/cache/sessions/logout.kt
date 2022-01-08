package io.ontola.cache.sessions

import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.proxySafeHeaders
import io.ontola.util.appendPath

suspend fun PipelineContext<Unit, ApplicationCall>.logout(): HttpResponse? {
    val logoutRequest = call.sessionManager.logoutRequest ?: return null

    val websiteIRI = call.tenant.websiteIRI
    val revokeUrl = URLBuilder(websiteIRI)
        .apply {
            appendPath("oauth", "revoke")
        }
        .build()

    return call.tenant.client.post(call.services.route(revokeUrl.encodedPath)) {
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            if (call.sessionManager.isUser) {
                set(HttpHeaders.Authorization, call.sessionManager.session!!.accessTokenBearer()!!)
            }
            proxySafeHeaders(call.request)
        }
        setBody(logoutRequest)
    }
}
