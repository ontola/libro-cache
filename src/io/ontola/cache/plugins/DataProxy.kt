package io.ontola

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.UnsafeHeadersList
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.parseHeaderValue
import io.ktor.request.ApplicationRequest
import io.ktor.request.accept
import io.ktor.request.document
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveChannel
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.filter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.plugins.tenant
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.util.copy
import java.util.Locale

private val DataProxyKey = AttributeKey<DataProxy>("DataProxyKey")

internal fun HttpRequestBuilder.proxyHeaders(
    call: ApplicationCall,
    session: SessionData?,
    useWebsiteIRI: Boolean = true,
) {
    val request = call.request
    val userToken = session?.accessToken

    headers {
        userToken?.let {
            header("Authorization", "Bearer $it")
        }
        if (useWebsiteIRI) {
            header("Website-IRI", call.tenant.websiteIRI)
        }
        copy("Accept", request)
        copy("Accept-Language", request)
        copy("Content-Type", request)
        copy("Origin", request)
        copy("Referer", request)
        copy("User-Agent", request)
        copy("X-Forwarded-Host", request)
        copy("X-Forwarded-Proto", request)
        copy("X-Forwarded-Ssl", request)
        copy("X-Real-Ip", request)
        copy("X-Requested-With", request)
        copy("X-Device-Id", request)
        copy("X-Request-Id", request)
        copy("X-Client-Ip", request)
        copy("Client-Ip", request)
        copy("Host", request)
        copy("Forwarded", request)
    }
}

class DataProxy(private val configuration: Configuration, val call: ApplicationCall?) {
    class Configuration {
        var transforms = mutableMapOf<Regex, (req: ApplicationRequest) -> String>()
        var contentTypes: List<ContentType> = emptyList()
        var extensions: List<String> = emptyList()
        var paths: List<String> = emptyList()
        val unsafeList = listOf(
            "new-authorization",
            "new-refresh-token",
            "content-length",
            "content-type",
            "transfer-encoding",
            "upgrade",
            *UnsafeHeadersList.toTypedArray()
        )
    }

    /**
     * Request a set of resources from the bulk API
     */
    suspend fun bulk(resources: List<Url>): String {
        val body = FormDataContent(
            Parameters.build {
                appendAll("resource[]", resources.map(Url::toString))
            }
        )
        val response = proxiedRequest(call!!, "/link-lib/bulk", HttpMethod.Post, body)

        return response.readText()
    }

    /**
     * Proxies the request to the data server
     */
    private suspend fun interceptHttp(call: ApplicationCall) {
        val originalReq = call.request

        val path = configuration.transforms.entries
            .find { (path, _) -> path.containsMatchIn(originalReq.uri) }
            ?.let { it.value(call.request) }
            ?: originalReq.uri

        val response = proxiedRequest(call, path, originalReq.httpMethod, call.receiveChannel())

        val proxiedHeaders = response.headers
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

        call.respond(
            object : OutgoingContent.WriteChannelContent() {
                override val contentLength: Long? = contentLength?.toLong()
                override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                override val headers: Headers = Headers.build {
                    response.headers["New-Authorization"]?.let {
                        call.sessionManager.setAuthorization(
                            accessToken = it,
                            refreshToken = response.headers["New-Refresh-Token"]
                                ?: throw Exception("Received New-Authorization header without New-Refresh-Header"),
                        )

                        val isRedirect = response.status.value in 300..399
                        if (!isRedirect) {
                            // if (hasAction(backendRes, 'https://ns.ontola.io/libro/actions/redirect')) {
                            //   return setActionParam(backendRes, 'https://ns.ontola.io/libro/actions/redirect', 'reload', 'true');
                            // }
                            this["Exec-Action"] = "https://ns.ontola.io/libro/actions/refresh"
                        }
                    }
                    appendAll(proxiedHeaders.filter { key, _ -> !configuration.unsafeList.contains(key.lowercase(Locale.getDefault())) })
                }
                override val status: HttpStatusCode? = response.status
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    response.content.copyAndClose(channel)
                }
            }
        )
    }

    private suspend fun proxiedRequest(call: ApplicationCall, path: String, method: HttpMethod, body: Any): HttpResponse {
        call.sessionManager.ensure()

        val client = HttpClient(CIO)
        return client.request(call.services.route(path)) {
            expectSuccess = false
            this.method = method
            this.body = body
            proxyHeaders(call, call.sessionManager.session, useWebsiteIRI = false)
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, DataProxy> {
        override val key = AttributeKey<DataProxy>("DataProxy")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DataProxy {
            val configuration = Configuration().apply(configure)
            val feature = DataProxy(configuration, null)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                val accept = parseHeaderValue(call.request.accept() ?: "*/*")
                    .map { v -> ContentType.parse(v.value).withoutParameters() }
                val path = call.request.path()

                this.call.attributes.put(DataProxyKey, DataProxy(configuration, this.call))

                val ext = if (path.contains(".")) path.split(".").lastOrNull() else null
                val shouldProxyHttp = call.request.document() != "manifest.json" &&
                    !configuration.paths.contains(call.request.path()) && (
                    ext?.let { configuration.extensions.contains(it) } ?: false ||
                        configuration.contentTypes.intersect(accept).isNotEmpty()
                    )

                call.logger.debug {
                    if (shouldProxyHttp)
                        "Proxying request to backend: $path"
                    else
                        "Processing request: $path"
                }

                if (shouldProxyHttp) {
                    feature.interceptHttp(this.call)
                    this.finish()
                }
            }

            return feature
        }
    }
}
