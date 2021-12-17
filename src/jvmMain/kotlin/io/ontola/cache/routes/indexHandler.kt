package io.ontola.cache.routes

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheRequest
import io.ontola.cache.bulk.coldHandler
import io.ontola.cache.bulk.collectResources
import io.ontola.cache.bulk.entriesToOutputStream
import io.ontola.cache.bulk.initHeaders
import io.ontola.cache.document.PageRenderContext
import io.ontola.cache.document.indexPage
import io.ontola.cache.document.pageRenderContextFromCall
import io.ontola.cache.isHTML
import io.ontola.cache.plugins.deviceId
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.VaryHeader
import io.ontola.cache.util.measured
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

data class HeadResponse(
    val statusCode: HttpStatusCode,
    val newAuthorization: String? = null,
    val newRefreshToken: String? = null,
    val accessControlAllowCredentials: String? = null,
    val accessControlAllowHeaders: String? = null,
    val accessControlAllowMethods: String? = null,
    val accessControlAllowOrigin: String? = null,
    val location: String? = null,
    val includeResources: List<String>? = emptyList(),
)

suspend fun PipelineContext<Unit, ApplicationCall>.headRequest(
    client: HttpClient,
    uri: String = call.request.uri,
    websiteBase: Url = call.tenant.websiteIRI,
): HeadResponse = measured("headRequest") {
    val lang = call.sessionManager.language
    val headResponse = client.head<HttpResponse>(call.services.route(uri)) {
        expectSuccess = false
        initHeaders(call, lang, websiteBase)
    }

    call.logger.debug { "Head response status ${headResponse.status}" }

    HeadResponse(
        headResponse.status,
        newAuthorization = headResponse.headers[CacheHttpHeaders.NewAuthorization],
        newRefreshToken = headResponse.headers[CacheHttpHeaders.NewRefreshToken],
        accessControlAllowCredentials = headResponse.headers[HttpHeaders.AccessControlAllowCredentials],
        accessControlAllowHeaders = headResponse.headers[HttpHeaders.AccessControlAllowHeaders],
        accessControlAllowMethods = headResponse.headers[HttpHeaders.AccessControlAllowMethods],
        accessControlAllowOrigin = headResponse.headers[HttpHeaders.AccessControlAllowOrigin],
        location = headResponse.headers[HttpHeaders.Location],
        includeResources = headResponse.headers[CacheHttpHeaders.IncludeResources]?.ifBlank { null }?.split(','),
    )
}

fun PipelineContext<Unit, ApplicationCall>.updateSessionAccessToken(head: HeadResponse) {
    if (head.newAuthorization != null && head.newRefreshToken == null) {
        throw Exception("refreshToken is missing while accessToken is present")
    }

    if (head.newAuthorization != null && head.newRefreshToken != null) {
        val newSession = SessionData(
            accessToken = head.newAuthorization,
            refreshToken = head.newRefreshToken,
            deviceId = call.deviceId,
        )

        call.sessions.set(newSession)
    }
}

val redirectStatuses = listOf(
    HttpStatusCode.MultipleChoices,
    HttpStatusCode.MovedPermanently,
    HttpStatusCode.Found,
    HttpStatusCode.SeeOther,
    HttpStatusCode.TemporaryRedirect,
    HttpStatusCode.PermanentRedirect,
)

fun HttpStatusCode.isRedirect(): Boolean = redirectStatuses.contains(this)

fun PipelineContext<Unit, ApplicationCall>.respondServerRedirect(head: HeadResponse) {
    if (head.location == null) {
        throw Exception("Trying to redirect with missing Location header.")
    }

    call.response.header(HttpHeaders.Location, head.location)
    call.response.status(head.statusCode)
}

suspend fun PipelineContext<Unit, ApplicationCall>.respondRenderWithData(
    ctx: PageRenderContext,
    includes: List<String>?,
    status: HttpStatusCode,
) {
    val updatedEntries = ByteArrayOutputStream().use { stream ->
        val entries = if (includes != null) {
            collectResources(includes.map { CacheRequest(it) })
                .also {
                    entriesToOutputStream(it, stream)
                }
                .filter { it.cacheControl != CacheControl.Private }
                .toList()
        } else {
            emptyList()
        }

        measured("render") {
            call.respondHtml(status) {
                ctx.seed = stream.toString(Charset.forName("UTF-8"))

                indexPage(ctx)
            }
        }

        entries
    }

    coldHandler(updatedEntries)
}

suspend fun PipelineContext<Unit, ApplicationCall>.indexHandler(client: HttpClient) {
    if (!call.request.isHTML()) {
        return call.respond(HttpStatusCode.NotFound)
    }

    call.response.header(HttpHeaders.Vary, VaryHeader)

    val head = headRequest(client)
    updateSessionAccessToken(head)

    if (head.statusCode.isRedirect()) {
        return respondServerRedirect(head)
    }

    val includes = listOf(
        call.tenant.websiteIRI.copy(encodedPath = call.request.uri).toString(),
        call.tenant.websiteIRI.toString() + "/ns/core",
    ) + (head.includeResources ?: emptyList())

    val ctx = call.pageRenderContextFromCall()

    respondRenderWithData(ctx, includes, head.statusCode)
}

fun Routing.mountIndex(client: HttpClient) {
    get("{...}") {
        indexHandler(client)
    }
}