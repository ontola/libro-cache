package io.ontola.cache.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentDisposition
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
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveChannel
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.filter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.Actions
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.VaryHeader
import io.ontola.cache.util.copy
import io.ontola.cache.util.hasAction
import io.ontola.cache.util.isDownloadRequest
import io.ontola.cache.util.isHtmlAccept
import io.ontola.cache.util.proxySafeHeaders
import io.ontola.cache.util.setActionParam
import io.ontola.cache.util.setParameter
import java.net.URLEncoder
import java.util.Locale

private val DataProxyKey = AttributeKey<DataProxy>("DataProxyKey")

internal fun HttpRequestBuilder.proxyHeaders(
    call: ApplicationCall,
    session: SessionData?,
    useWebsiteIRI: Boolean = true,
) {
    val request = call.request

    headers {
        session?.accessTokenBearer()?.let {
            header(HttpHeaders.Authorization, it)
        }
        if (useWebsiteIRI) {
            header(CacheHttpHeaders.WebsiteIri, call.tenant.websiteIRI)
        }
        proxySafeHeaders(request)
        copy(HttpHeaders.Accept, request)
        copy(HttpHeaders.ContentType, request)
        copy(HttpHeaders.Forwarded, request)
        copy(HttpHeaders.Host, request)
        copy(HttpHeaders.Origin, request)
        copy(HttpHeaders.Referrer, request)
        copy(HttpHeaders.UserAgent, request)
        copy(CacheHttpHeaders.XDeviceId, request)

        copy("Client-Ip", request)
        copy("X-Client-Ip", request)
        copy("X-Real-Ip", request)
        copy("X-Requested-With", request)
    }
}

class DataProxy(private val configuration: Configuration, val call: ApplicationCall?) {
    class Configuration {
        var transforms = mutableMapOf<Regex, (req: ApplicationRequest) -> String>()
        var binaryPaths: List<String> = emptyList()
        var contentTypes: List<ContentType> = emptyList()
        var extensions: List<String> = emptyList()
        var excludedPaths: List<String> = emptyList()
        val unsafeList = listOf(
            CacheHttpHeaders.NewAuthorization.lowercase(),
            CacheHttpHeaders.NewRefreshToken.lowercase(),
            HttpHeaders.ContentLength.lowercase(),
            HttpHeaders.ContentType.lowercase(),
            HttpHeaders.TransferEncoding.lowercase(),
            HttpHeaders.Upgrade.lowercase(),
            *UnsafeHeadersList.toTypedArray()
        )
        val client = HttpClient(CIO) {
            followRedirects = false
            expectSuccess = false
        }

        fun isBinaryRequest(uri: Url, accept: String?): Boolean {
            return (binaryPaths.any { uri.encodedPath.contains(it) } && !accept.isHtmlAccept()) || uri.isDownloadRequest()
        }
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

    fun ApplicationCall.newAuthorizationBulk(response: HttpResponse): String? {
        val newAuthorization = response.headers[CacheHttpHeaders.NewAuthorization] ?: return null

        sessionManager.setAuthorization(
            accessToken = newAuthorization,
            refreshToken = response.headers[CacheHttpHeaders.NewRefreshToken]
                ?: throw Exception("Received New-Authorization header without New-Refresh-Header"),
        )

        val isRedirect = (response.status.value in 300..399)

        if (isRedirect) {
            return null
        }

        if (response.hasAction(Actions.RedirectAction)) {
            return response.setActionParam(Actions.RedirectAction, "reload", "true")
        }

        val location = response.headers[HttpHeaders.Location]
        if (location != null && location.isNotEmpty()) {
            return Actions.RedirectAction
                .setParameter("reload", "true")
                .setParameter("location", URLEncoder.encode(location, "utf-8"))
        }

        return Actions.RefreshAction
    }

    /**
     * Proxies the request to the data server
     */
    private suspend fun interceptRequest(call: ApplicationCall) {
        val originalReq = call.request
        val uri = Url(originalReq.uri)

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
                    set(HttpHeaders.Vary, VaryHeader)

                    if (uri.isDownloadRequest()) {
                        append(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.disposition)
                    }

                    val action = call.newAuthorizationBulk(response)

                    if (action != null) {
                        set(CacheHttpHeaders.ExecAction, action)
                    }

                    appendAll(proxiedHeaders.filter { key, _ -> !configuration.unsafeList.contains(key.lowercase(Locale.getDefault())) })
                }
                override val status: HttpStatusCode = response.status
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    response.content.copyAndClose(channel)
                }
            }
        )
    }

    private suspend fun proxiedRequest(call: ApplicationCall, path: String, method: HttpMethod, body: Any): HttpResponse {
        call.sessionManager.ensure()

        return configuration.client.request(call.services.route(path)) {
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
                val uri = Url(call.request.uri)
                val path = call.request.path()

                call.attributes.put(DataProxyKey, DataProxy(configuration, this.call))

                val isManifestRequest = call.request.document() == "manifest.json"
                val isPathExcluded = configuration.excludedPaths.contains(path)
                val isNotExcluded = !isManifestRequest && !isPathExcluded

                val ext = if (path.contains(".")) path.split(".").lastOrNull() else null
                val isDataReqByExtension = ext?.let { configuration.extensions.contains(it) } ?: false
                val isDataReqByAccept = configuration.contentTypes.intersect(accept).isNotEmpty()
                val isBinaryRequest = configuration.isBinaryRequest(uri, call.request.header(HttpHeaders.Accept))
                val isProxyableComponent = isDataReqByExtension || isDataReqByAccept || isBinaryRequest

                val shouldProxyHttp = isNotExcluded && isProxyableComponent

                call.logger.debug {
                    if (shouldProxyHttp)
                        "Proxying request to backend: $path"
                    else
                        "Processing request: $path"
                }

                if (shouldProxyHttp) {
                    feature.interceptRequest(this.call)
                    this.finish()
                }
            }

            return feature
        }
    }
}
