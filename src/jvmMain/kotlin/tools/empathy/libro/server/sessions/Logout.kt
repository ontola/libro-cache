package tools.empathy.libro.server.sessions

import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.tenantization.TenantData
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.proxySafeHeaders
import tools.empathy.url.appendPath

suspend fun PipelineContext<Unit, ApplicationCall>.logout(): HttpResponse? {
    if (call.tenant is TenantData.Local) {
        return null
    }

    val logoutRequest = call.sessionManager.logoutRequest ?: return null

    val websiteIRI = call.tenant.websiteIRI
    val revokeUrl = websiteIRI.appendPath("oauth", "revoke")

    return (call.tenant as TenantData.External).client.post(call.services.route(revokeUrl.encodedPath)) {
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
