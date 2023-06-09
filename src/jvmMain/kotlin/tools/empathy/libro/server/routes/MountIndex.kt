package tools.empathy.libro.server.routes

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.head
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.fullPath
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.css.head
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.bulk.CacheControl
import tools.empathy.libro.server.bulk.CacheRequest
import tools.empathy.libro.server.bulk.coldHandler
import tools.empathy.libro.server.bulk.collectResources
import tools.empathy.libro.server.bulk.entriesToOutputStream
import tools.empathy.libro.server.bulk.initHeaders
import tools.empathy.libro.server.csp.nonce
import tools.empathy.libro.server.document.PageRenderContext
import tools.empathy.libro.server.document.indexPage
import tools.empathy.libro.server.document.pageRenderContextFromCall
import tools.empathy.libro.server.isHTML
import tools.empathy.libro.server.plugins.deviceId
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.logger
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.sessions.SessionData
import tools.empathy.libro.server.sessions.TokenPair
import tools.empathy.libro.server.tenantization.TenantData
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.VaryHeader
import tools.empathy.libro.server.util.measured
import tools.empathy.serialization.DataSlice
import tools.empathy.studio.StudioDeploymentKey
import tools.empathy.url.appendPath
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

fun Routing.mountIndex() {
    get("{...}") {
        call.indexHandler()
    }
}

private suspend fun ApplicationCall.indexHandler() {
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

    when (val t = tenant) {
        is TenantData.Local -> localIndexHandler(t)
        is TenantData.External -> externalIndexHandler(t)
    }
}

private suspend fun ApplicationCall.localIndexHandler(tenant: TenantData.Local) {
    val context = tenant.context.invoke(this)
    val requestedId = URLBuilder(tenant.websiteIRI).apply { encodedPath = request.uri }.build()
    val idInData = context.data?.containsKey(requestedId.toString()) == true || context.data?.containsKey(requestedId.fullPath) == true
    val status = if (idInData) HttpStatusCode.OK else HttpStatusCode.NotFound
    val ctx = context.apply {
        this.nonce = this@localIndexHandler.nonce
    }
    val includes = context.data?.keys?.toList() ?: emptyList()

    respondRenderWithData(ctx, includes, status)
}

private suspend fun ApplicationCall.externalIndexHandler(tenant: TenantData.External) {
    val head = headRequest(tenant.client)
    updateSessionAccessToken(head)

    if (head.statusCode.isRedirect()) {
        return respondServerRedirect(head)
    }

    val includes = listOf(
        URLBuilder(tenant.websiteIRI).apply { encodedPath = request.uri }.buildString(),
        tenant.websiteIRI.appendPath("ns", "core").toString(),
    ) + (head.includeResources ?: emptyList())

    val ctx = pageRenderContextFromCall().apply {
        this.nonce = this@externalIndexHandler.nonce
    }

    respondRenderWithData(ctx, includes, head.statusCode)
}

suspend fun ApplicationCall.headRequest(
    client: HttpClient,
    uri: String = request.uri,
    websiteBase: Url = tenant.websiteIRI,
): HeadResponse = measured("headRequest") {
    val lang = language
    val headResponse = client.head(services.route(uri)) {
        expectSuccess = false
        initHeaders(this@headRequest, lang, websiteBase)
    }

    logger.debug { "Head response status ${headResponse.status}" }

    HeadResponse(
        headResponse.status,
        newAuthorization = headResponse.headers[LibroHttpHeaders.NewAuthorization],
        newRefreshToken = headResponse.headers[LibroHttpHeaders.NewRefreshToken],
        accessControlAllowCredentials = headResponse.headers[HttpHeaders.AccessControlAllowCredentials],
        accessControlAllowHeaders = headResponse.headers[HttpHeaders.AccessControlAllowHeaders],
        accessControlAllowMethods = headResponse.headers[HttpHeaders.AccessControlAllowMethods],
        accessControlAllowOrigin = headResponse.headers[HttpHeaders.AccessControlAllowOrigin],
        location = headResponse.headers[HttpHeaders.Location],
        includeResources = headResponse.headers[LibroHttpHeaders.IncludeResources]?.ifBlank { null }?.split(','),
    )
}

private fun ApplicationCall.updateSessionAccessToken(head: HeadResponse) {
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

private val redirectStatuses = listOf(
    HttpStatusCode.MultipleChoices,
    HttpStatusCode.MovedPermanently,
    HttpStatusCode.Found,
    HttpStatusCode.SeeOther,
    HttpStatusCode.TemporaryRedirect,
    HttpStatusCode.PermanentRedirect,
)

private fun HttpStatusCode.isRedirect(): Boolean = redirectStatuses.contains(this)

private fun ApplicationCall.respondServerRedirect(head: HeadResponse) {
    if (head.location == null) {
        throw Exception("Trying to redirect with missing Location header.")
    }

    response.header(HttpHeaders.Location, head.location)
    response.status(head.statusCode)
}

private suspend fun ApplicationCall.respondRenderWithData(
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
                            curr?.forEach { entry -> acc.merge(entry.key, entry.value) { new, _ -> new } }
                            acc
                        }
                }

                indexPage(ctx)
            }
        }

        entries
    }

    coldHandler(updatedEntries)
}
