
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.SPIAuthorizeRequest
import io.ontola.cache.bulk.SPIResourceResponseItem
import io.ontola.cache.configureClient
import io.ontola.cache.plugins.SessionsConfig
import io.ontola.cache.routes.HeadResponse
import io.ontola.cache.sessions.OIDCTokenResponse
import io.ontola.cache.sessions.UserType
import io.ontola.cache.tenantization.TenantFinderResponse
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.fullUrl
import io.ontola.cache.util.withoutProto
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset
import java.util.Date
import kotlin.time.Duration.Companion.hours

private val jsonContentTypeHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

data class TestClientBuilder(
    val resources: MutableList<Triple<String, String, CacheControl>> = mutableListOf(),
    private val headResponses: MutableMap<Url, HeadResponse> = mutableMapOf(),
) {
    var newToken: Pair<String, String>? = null

    fun setHeadResponse(url: Url, response: HeadResponse): TestClientBuilder {
        headResponses[url] = response

        return this
    }

    fun setNewAuthorization(accessToken: String, refreshToken: String) {
        newToken = Pair(accessToken, refreshToken)
    }

    fun build(): HttpClient = HttpClient(MockEngine) {
        configureClient()
        engine {
            addHandler { request ->
                if (request.method == HttpMethod.Head) {
                    return@addHandler handleHeadRequest(request, headResponses)
                }

                when (request.url.encodedPath) {
                    "/_public/spi/find_tenant" -> handleFindTenantRequest(request)
                    "/spi/bulk" -> handleBulkRequest(request, resources, newToken)
                    "/oauth/token" -> handleTokenRequest()
                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun MockRequestHandleScope.handleHeadRequest(request: HttpRequestData, headResponses: MutableMap<Url, HeadResponse>): HttpResponseData {
    val path = request.url.fullPath
    val websiteIRI = Url(request.headers["Website-IRI"] ?: return respond("", HttpStatusCode.NotFound))
    val response = headResponses[Url("$websiteIRI$path")] ?: return respond("", HttpStatusCode.NotFound)

    val test = buildMap {
        response.newAuthorization?.let {
            put(CacheHttpHeaders.NewAuthorization, listOf(it))
        }
        response.newRefreshToken?.let {
            put(CacheHttpHeaders.NewRefreshToken, listOf(it))
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
            put(CacheHttpHeaders.IncludeResources, listOf(it.joinToString(",")))
        }
    }

    return respond("", response.statusCode, headersOf(*test.toList().toTypedArray()))
}

@OptIn(ExperimentalSerializationApi::class)
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

@OptIn(ExperimentalSerializationApi::class)
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
            CacheHttpHeaders.NewAuthorization to listOf(newToken.first),
            CacheHttpHeaders.NewRefreshToken to listOf(newToken.second),
            *jsonContentTypeHeaders.entries().map { it.key to it.value }.toTypedArray(),
        )
    }

    return respond(
        Json.encodeToString(payload),
        HttpStatusCode.OK,
        headers = headers,
    )
}

private fun MockRequestHandleScope.handleTokenRequest(): HttpResponseData {
    val config = SessionsConfig.forTesting()

    val accessToken = JWT
        .create()
        .withClaim("application_id", config.clientId)
        .withIssuedAt(Date.from(Clock.System.now().toJavaInstant()))
        .withExpiresAt(Date.from(Clock.System.now().plus(1.hours).toJavaInstant()))
        .withClaim("scopes", listOf("user"))
        .withClaim(
            "user",
            mapOf(
                "type" to UserType.Guest.name.lowercase(),
                "iri" to "",
                "@id" to "",
                "id" to "",
                "language" to "en",
            )
        )
        .withClaim("application_id", config.clientId)
        .sign(Algorithm.HMAC512(config.jwtEncryptionToken))
    val refreshToken = JWT
        .create()
        .withClaim("application_id", config.clientId)
        .sign(Algorithm.HMAC512(config.jwtEncryptionToken))
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
