package io.ontola.cache.dataproxy

import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.util.isDownloadRequest

private val DataProxyKey = AttributeKey<DataProxy>("DataProxyKey")

private fun Configuration.proxiedUri(
    originalReq: ApplicationRequest,
    call: ApplicationCall
): String = transforms
    .entries
    .find { (path, _) -> path.containsMatchIn(originalReq.uri) }
    ?.let { it.value(call.request) }
    ?: originalReq.uri

class DataProxy(private val config: Configuration, val call: ApplicationCall?) {
    /**
     * Request a set of resources from the bulk API
     */
    suspend fun bulk(resources: List<Url>): String {
        val body = FormDataContent(
            Parameters.build {
                appendAll("resource[]", resources.map(Url::toString))
            }
        )
        val response = config.proxiedRequest(call!!, "/link-lib/bulk", HttpMethod.Post, body)

        return response.bodyAsText()
    }

    /**
     * Proxies the request to the data server
     */
    @OptIn(InternalAPI::class)
    private suspend fun interceptRequest(call: ApplicationCall) {
        val originalReq = call.request
        val uri = Url(originalReq.uri)
        val isDownloadRequest = uri.isDownloadRequest()

        val requestUri = config.proxiedUri(originalReq, call)
        val response = config.proxiedRequest(call, requestUri, originalReq.httpMethod, call.receiveChannel())
        val rule = config.matchOrDefault(requestUri)

        val proxiedHeaders = response.headers
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]
        val headerBuilder = when (rule.includeCredentials) {
            true -> ::trustedProxyHeaders
            false -> ::untrustedProxyHeaders
        }

        call.respond(
            object : OutgoingContent.WriteChannelContent() {
                override val contentLength: Long? = contentLength?.toLong()
                override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                override val headers: Headers = headerBuilder(
                    config,
                    isDownloadRequest,
                    proxiedHeaders,
                    call.sessionManager::setAuthorization,
                    response,
                )

                override val status: HttpStatusCode = response.status
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    response.bodyAsChannel().copyAndClose(channel)
                }
            }
        )
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, DataProxy> {
        override val key = AttributeKey<DataProxy>("DataProxy")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DataProxy {
            val configuration = Configuration().apply(configure)
            val feature = DataProxy(configuration, null)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                call.attributes.put(DataProxyKey, DataProxy(configuration, call))
                val shouldProxyHttp = configuration.shouldProxy(call.request)

                call.logger.debug {
                    val uri = call.request.uri
                    val rule = configuration.matchOrDefault(uri)

                    if (shouldProxyHttp)
                        "Proxying request to backend: $uri, rule: $rule"
                    else
                        "Processing request: $uri, rule: $rule"
                }

                if (shouldProxyHttp) {
                    feature.interceptRequest(call)
                    finish()
                }
            }

            return feature
        }
    }
}
