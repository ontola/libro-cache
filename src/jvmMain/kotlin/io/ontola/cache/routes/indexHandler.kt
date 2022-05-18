package io.ontola.cache.routes

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheRequest
import io.ontola.cache.bulk.coldHandler
import io.ontola.cache.bulk.collectResources
import io.ontola.cache.bulk.entriesToOutputStream
import io.ontola.cache.bulk.initHeaders
import io.ontola.cache.csp.nonce
import io.ontola.cache.document.PageRenderContext
import io.ontola.cache.document.indexPage
import io.ontola.cache.document.pageRenderContextFromCall
import io.ontola.cache.initializeOpenTelemetry
import io.ontola.cache.isHTML
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.deviceId
import io.ontola.cache.plugins.language
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.services
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.sessions.TokenPair
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.VaryHeader
import io.ontola.cache.util.measured
import io.ontola.cache.util.withSpan
import io.ontola.empathy.web.DataSlice
import io.ontola.studio.StudioDeploymentKey
import io.ontola.util.appendPath
import io.opentelemetry.context.Context
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

suspend fun ApplicationCall.headRequest(
    client: HttpClient,
    uri: String = request.uri,
    websiteBase: Url = tenant.websiteIRI,
): HeadResponse = measured("headRequest") { span ->
    span.setAttribute(SemanticAttributes.HTTP_METHOD, "POST")
    span.setAttribute(SemanticAttributes.HTTP_URL, uri)

    val lang = language
    val headResponse = client.head(services.route(uri)) {
        expectSuccess = false
        initHeaders(this@headRequest, lang, websiteBase)
        initializeOpenTelemetry().propagators.textMapPropagator.inject(Context.current(), request) { _, key, value ->
            header(key, value)
            if (key == "traceparent") {
                header("trace-parent", value)
            }
        }
    }

    logger.debug { "Head response status ${headResponse.status}" }

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

fun ApplicationCall.updateSessionAccessToken(head: HeadResponse) {
    if (head.newAuthorization != null && head.newRefreshToken == null) {
        throw Exception("refreshToken is missing while accessToken is present")
    }

    if (head.newAuthorization != null && head.newRefreshToken != null) {
        val existing = sessions.get<SessionData>() ?: SessionData()
        val newSession = existing.copy(
            credentials = TokenPair(
                accessToken = head.newAuthorization,
                refreshToken = head.newRefreshToken,
            ),
            deviceId = deviceId,
        )

        sessions.set(newSession)
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

fun ApplicationCall.respondServerRedirect(head: HeadResponse) {
    if (head.location == null) {
        throw Exception("Trying to redirect with missing Location header.")
    }

    response.header(HttpHeaders.Location, head.location)
    response.status(head.statusCode)
}

suspend fun ApplicationCall.respondRenderWithData(
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
            respondHtml(status) {
                if (ctx.data == null) {
                    withSpan("render", "generateseed") {
                        ctx.data = stream
                            .toString(Charset.forName("UTF-8"))
                            .split("\n")
                            .filter { it.isNotBlank() }
                            .map {
                                try {
                                    Json.decodeFromString<DataSlice>(it)
                                } catch (e: SerializationException) {
                                    null
                                }
                            }
                            .fold(mutableMapOf()) { acc, curr ->
                                // TODO merge values
                                curr?.forEach { entry -> acc.merge(entry.key, entry.value) { new, old -> new } }
                                acc
                            }
                    }
                }
                withSpan("render", "html") {
                    indexPage(ctx)
                }
            }
        }

        entries
    }

    coldHandler(updatedEntries)
}

suspend fun ApplicationCall.indexHandler(client: HttpClient) {
    if (attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
        return
    }

    if (!request.isHTML()) {
        return respond(HttpStatusCode.NotFound)
    }

    if (attributes.getOrNull(StudioDeploymentKey) != null) {
        val deployment = attributes[StudioDeploymentKey].apply {
            nonce = this@indexHandler.nonce
        }

        return respondRenderWithData(deployment, emptyList(), HttpStatusCode.OK)
    }

    response.header(HttpHeaders.Vary, VaryHeader)

    val head = headRequest(client)
    updateSessionAccessToken(head)

    if (head.statusCode.isRedirect()) {
        return respondServerRedirect(head)
    }

    val includes = listOf(
        URLBuilder(tenant.websiteIRI).apply { encodedPath = request.uri }.buildString(),
        tenant.websiteIRI.appendPath("ns", "core").toString(),
    ) + (head.includeResources ?: emptyList())

    val ctx = pageRenderContextFromCall().apply {
        this.nonce = this@indexHandler.nonce
    }

    respondRenderWithData(ctx, includes, head.statusCode)
}

fun Routing.mountIndex() {
    get("{...}") {
        call.indexHandler(call.application.cacheConfig.client)
    }
}
