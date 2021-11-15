
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
import io.ontola.cache.HeadResponse
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.SPIAuthorizeRequest
import io.ontola.cache.bulk.SPIResourceResponseItem
import io.ontola.cache.configureClient
import io.ontola.cache.plugins.TenantFinderResponse
import io.ontola.cache.util.fullUrl
import io.ontola.cache.util.withoutProto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

data class TestClientBuilder(
    private val resources: MutableList<Triple<String, String, CacheControl>> = mutableListOf(),
    private val headResponses: MutableMap<Url, HeadResponse> = mutableMapOf(),
) {
    fun setHeadResponse(url: Url, response: HeadResponse): TestClientBuilder {
        headResponses[url] = response

        return this
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
                    "/spi/bulk" -> handleBulkRequest(request, resources)
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

    val test = buildMap<String, List<String>> {
        response.newAuthorization?.let {
            put("new-authorization", listOf(it))
        }
        response.newRefreshToken?.let {
            put("new-refresh-token", listOf(it))
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
            put("Include-Resources", listOf(it.joinToString(",")))
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
    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    return respond(
        Json.encodeToString(payload),
        HttpStatusCode.OK,
        headers = responseHeaders,
    )
}

@OptIn(ExperimentalSerializationApi::class)
private suspend fun MockRequestHandleScope.handleBulkRequest(
    request: HttpRequestData,
    resources: List<Triple<String, String, CacheControl>>,
): HttpResponseData {
    val body = request.body.toByteArray().toString(Charset.defaultCharset())
    val requestPayload = Json.decodeFromString<SPIAuthorizeRequest>(body)
    val foundResources = resources.map { it.first }.intersect(requestPayload.resources.map { it.iri })

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
    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    return respond(
        Json.encodeToString(payload),
        HttpStatusCode.OK,
        headers = responseHeaders,
    )
}
