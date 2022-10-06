
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.bulk.CacheControl
import tools.empathy.libro.server.bulk.SPIAuthorizeRequest
import tools.empathy.libro.server.bulk.SPIResourceResponseItem
import tools.empathy.libro.server.configureClient
import tools.empathy.libro.server.routes.HeadResponse
import tools.empathy.libro.server.sessions.LogoutRequest
import tools.empathy.libro.server.sessions.OIDCRequest
import tools.empathy.libro.server.sessions.OIDCTokenResponse
import tools.empathy.libro.server.tenantization.TenantFinderResponse
import tools.empathy.libro.server.tenantization.TenantsResponse
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.url.appendPath
import tools.empathy.url.asHref
import tools.empathy.url.fullUrl
import tools.empathy.url.withoutProto
import java.nio.charset.Charset

class ClientState(
    var refreshTokenSuccess: Boolean = true,
    var initialKeys: Pair<String, String>? = null,
    val resources: MutableList<Triple<String, String, CacheControl>> = mutableListOf(),
    val manifests: MutableMap<Url, Manifest> = mutableMapOf(),
    val headResponses: MutableMap<Url, HeadResponse> = mutableMapOf(),
    var newToken: Pair<String, String>? = null
) {
    var nextKeys: Pair<String, String> = initialKeys ?: generateTestAccessTokenPair()
        get() = initialKeys?.also { initialKeys = null } ?: generateTestAccessTokenPair()
}

private val jsonContentTypeHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

data class TestClientBuilder(
    val configure: ClientState.() -> Unit,
) {
    internal val config = ClientState().apply(configure)

    fun addResources(resources: List<Triple<String, String, CacheControl>>) {
        config.resources.addAll(resources)
    }

    fun setHeadResponse(url: Url, response: HeadResponse): TestClientBuilder {
        config.headResponses[url] = response

        return this
    }

    fun addManifest(website: Url, manifest: Manifest) {
        config.manifests[website.asHref] = manifest
    }

    fun setNewAuthorization(accessToken: String, refreshToken: String) {
        config.newToken = Pair(accessToken, refreshToken)
    }

    fun setRefreshTokenSuccess(value: Boolean) {
        config.refreshTokenSuccess = value
    }

    fun build(): HttpClient = HttpClient(MockEngine) {
        configureClient()
        engine {
            addHandler { request ->
                if (request.method == HttpMethod.Head) {
                    return@addHandler handleHeadRequest(request, config.headResponses)
                }
                val websitePaths = config.manifests.keys.map { it.appendPath("manifest.json").encodedPath }.toSet()

                when (request.url.encodedPath) {
                    "/_public/spi/find_tenant" -> handleFindTenantRequest(request)
                    "/_public/spi/tenants" -> handleTenantsRequest(request)
                    "/spi/bulk" -> handleBulkRequest(request, config.resources, config.newToken)
                    "/oauth/token" -> handleTokenRequest(request, config)
                    "/oauth/revoke" -> handleTokenRevocation(request)
                    in websitePaths -> handleManifestRequest(request, config)
                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun MockRequestHandleScope.handleHeadRequest(request: HttpRequestData, headResponses: MutableMap<Url, HeadResponse>): HttpResponseData {
    val path = request.url.fullPath
    val websiteIRI = Url(request.headers[LibroHttpHeaders.WebsiteIri] ?: return respond("", HttpStatusCode.NotFound))
    val response = headResponses[Url("$websiteIRI$path")] ?: return respond("", HttpStatusCode.NotFound)

    val test = buildMap {
        response.newAuthorization?.let {
            put(LibroHttpHeaders.NewAuthorization, listOf(it))
        }
        response.newRefreshToken?.let {
            put(LibroHttpHeaders.NewRefreshToken, listOf(it))
        }
        response.accessControlAllowCredentials?.let {
            put(HttpHeaders.AccessControlAllowCredentials, listOf(it))
        }
        response.accessControlAllowHeaders?.let {
            put(HttpHeaders.AccessControlAllowHeaders, listOf(it))
        }
        response.accessControlAllowMethods?.let {
            put(HttpHeaders.AccessControlAllowMethods, listOf(it))
        }
        response.accessControlAllowOrigin?.let {
            put(HttpHeaders.AccessControlAllowOrigin, listOf(it))
        }
        response.location?.let {
            put(HttpHeaders.Location, listOf(it))
        }
        response.includeResources?.let {
            put(LibroHttpHeaders.IncludeResources, listOf(it.joinToString(",")))
        }
    }

    return respond("", response.statusCode, headersOf(*test.toList().toTypedArray()))
}

private fun MockRequestHandleScope.handleFindTenantRequest(request: HttpRequestData): HttpResponseData {
    val iri = request.url.parameters["iri"] ?: throw Exception("Tenant finder request without IRI")
    val payload = TenantFinderResponse(
        iriPrefix = Url(iri).withoutProto(),
    )

    return respond(
        Json.encodeToString(payload),
        HttpStatusCode.OK,
        headers = jsonContentTypeHeaders,
    )
}

private fun MockRequestHandleScope.handleTenantsRequest(request: HttpRequestData): HttpResponseData {
    val payload = TenantsResponse(
        sites = emptyList()
    )

    return respond(
        Json.encodeToString(payload),
        HttpStatusCode.OK,
        headers = jsonContentTypeHeaders,
    )
}

private suspend fun MockRequestHandleScope.handleBulkRequest(
    request: HttpRequestData,
    resources: List<Triple<String, String, CacheControl>>,
    newToken: Pair<String, String>?,
): HttpResponseData {
    val body = request.body.toByteArray().toString(Charset.defaultCharset())
    val requestPayload = Json.decodeFromString<SPIAuthorizeRequest>(body)
    val foundResources = resources.map { it.first }.intersect(requestPayload.resources.map { it.iri }.toSet())

    val payload = foundResources.map {
        val (iri, payload, cacheControl) = resources.find { (key) -> key == it }!!
        SPIResourceResponseItem(
            iri = iri,
            status = HttpStatusCode.OK.value,
            cache = cacheControl,
            language = "en",
            body = payload,
        )
    }

    val headers = if (newToken == null) {
        jsonContentTypeHeaders
    } else {
        headersOf(
            LibroHttpHeaders.NewAuthorization to listOf(newToken.first),
            LibroHttpHeaders.NewRefreshToken to listOf(newToken.second),
            *jsonContentTypeHeaders.entries().map { it.key to it.value }.toTypedArray(),
        )
    }

    return respond(
        Json.encodeToString(payload),
        HttpStatusCode.OK,
        headers = headers,
    )
}

private fun MockRequestHandleScope.handleManifestRequest(request: HttpRequestData, config: ClientState): HttpResponseData {
    val origin = request.headers[LibroHttpHeaders.WebsiteIri]
    val path = request.url.encodedPath.removeSuffix("/manifest.json")
    val manifest = config.manifests[Url("$origin$path")] ?: return respond("", HttpStatusCode.NotFound)

    return respond(
        Json.encodeToString(manifest),
        HttpStatusCode.OK,
        jsonContentTypeHeaders,
    )
}

private suspend fun MockRequestHandleScope.handleTokenRevocation(request: HttpRequestData): HttpResponseData {
    Json.decodeFromString<LogoutRequest>(request.body.toByteArray().toString(Charset.defaultCharset()))

    return respond("", HttpStatusCode.OK)
}

private suspend fun MockRequestHandleScope.handleTokenRequest(request: HttpRequestData, config: ClientState): HttpResponseData {
    val requestBody = Json.decodeFromString<OIDCRequest>(request.body.toByteArray().decodeToString())

    if (requestBody.scope?.contains("guest") == true && !config.refreshTokenSuccess) {
        val error = mapOf(
            "error" to "invalid_grant",
            "error_description" to "The provided authorization grant is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client.",
        )

        return respond(
            Json.encodeToString(error),
            HttpStatusCode.BadRequest,
            jsonContentTypeHeaders,
        )
    }

    val (accessToken, refreshToken) = config.nextKeys

    val body = OIDCTokenResponse(
        accessToken = accessToken,
        tokenType = "",
        expiresIn = 100,
        refreshToken = refreshToken,
        scope = "user",
        createdAt = Clock.System.now().epochSeconds,
    )

    return respond(
        Json.encodeToString(body),
        HttpStatusCode.OK,
        headers = jsonContentTypeHeaders,
    )
}
