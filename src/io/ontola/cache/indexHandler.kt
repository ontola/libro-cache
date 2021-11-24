package io.ontola.cache

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.bulk.CacheRequest
import io.ontola.cache.bulk.coldHandler
import io.ontola.cache.bulk.initHeaders
import io.ontola.cache.bulk.resourcesToOutputStream
import io.ontola.cache.document.AssetsManifests
import io.ontola.cache.document.PageConfiguration
import io.ontola.cache.document.indexPage
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.plugins.tenant
import io.ontola.cache.sessions.SessionData
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

suspend fun PipelineContext<Unit, ApplicationCall>.headResponse(client: HttpClient): HeadResponse {
    val lang = call.sessionManager.language
    val headResponse = client.head<HttpResponse>(call.services.route(call.request.uri)) {
        header("Website-IRI", call.tenant.websiteIRI)
        initHeaders(call, lang)
    }

    call.logger.debug { "Head response status ${headResponse.status}" }

    return HeadResponse(
        headResponse.status,
        newAuthorization = headResponse.headers["new-authorization"],
        newRefreshToken = headResponse.headers["new-refresh-token"],
        accessControlAllowCredentials = headResponse.headers[HttpHeaders.AccessControlAllowCredentials],
        accessControlAllowHeaders = headResponse.headers[HttpHeaders.AccessControlAllowHeaders],
        accessControlAllowMethods = headResponse.headers[HttpHeaders.AccessControlAllowMethods],
        accessControlAllowOrigin = headResponse.headers[HttpHeaders.AccessControlAllowOrigin],
        location = headResponse.headers[HttpHeaders.Location],
        includeResources = headResponse.headers["Include-Resources"]?.ifBlank { null }?.split(','),
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
    call.response.status(HttpStatusCode.TemporaryRedirect)
}

suspend fun PipelineContext<Unit, ApplicationCall>.respondRenderWithData(
    includes: List<String>?,
    assets: AssetsManifests,
) {
    val manifest = call.tenant.manifest

    val pageConfig = PageConfiguration(appElement = "root", assets = assets)
    val updatedEntries = ByteArrayOutputStream().use { stream ->
        val entries = if (includes != null) {
            resourcesToOutputStream(includes.map { CacheRequest(it) }, stream)
        } else {
            emptyList()
        }

        call.respondHtml(HttpStatusCode.OK) {
            indexPage(
                pageConfig,
                manifest,
                stream.toString(Charset.forName("UTF-8")),
                call.application.cacheConfig.serializer
            )
        }

        entries
    }

    coldHandler(updatedEntries)
}

suspend fun PipelineContext<Unit, ApplicationCall>.indexHandler(client: HttpClient, assets: AssetsManifests) {
    if (!call.request.isHTML()) {
        return call.respond(HttpStatusCode.NotFound)
    }

    val head = headResponse(client)
    updateSessionAccessToken(head)
    call.response.status(head.statusCode)

    if (head.statusCode.isRedirect()) {
        return respondServerRedirect(head)
    }

    val includes = listOf(
        call.tenant.websiteIRI.copy(encodedPath = call.request.uri).toString(),
        call.tenant.websiteIRI.toString() + "/ns/core",
    ) + (head.includeResources ?: emptyList())

    respondRenderWithData(includes, assets)
}
