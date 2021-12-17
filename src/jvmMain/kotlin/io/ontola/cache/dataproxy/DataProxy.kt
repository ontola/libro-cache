package io.ontola.cache.dataproxy

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.readText
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.request.ApplicationRequest
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
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.VaryHeader
import io.ontola.cache.util.isDownloadRequest
import java.util.Locale

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

        return response.readText()
    }

    /**
     * Proxies the request to the data server
     */
    private suspend fun interceptRequest(call: ApplicationCall) {
        val originalReq = call.request
        val uri = Url(originalReq.uri)

        val requestUri = config.proxiedUri(originalReq, call)
        val response = config.proxiedRequest(call, requestUri, originalReq.httpMethod, call.receiveChannel())

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

                    newAuthorizationBulk(response)?.let {
                        call.sessionManager.setAuthorization(
                            it.accessToken,
                            it.refreshToken,
                        )

                        it.action?.let { action ->
                            set(CacheHttpHeaders.ExecAction, action)
                        }
                    }

                    appendAll(proxiedHeaders.filter { key, _ ->
                        !config.unsafeList.contains(key.lowercase(Locale.getDefault()))
                    })
                }
                override val status: HttpStatusCode = response.status
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    response.content.copyAndClose(channel)
                }
            }
        )
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, DataProxy> {
        override val key = AttributeKey<DataProxy>("DataProxy")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DataProxy {
            val configuration = Configuration().apply(configure)
            val feature = DataProxy(configuration, null)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                call.attributes.put(DataProxyKey, DataProxy(configuration, call))
                val shouldProxyHttp = configuration.shouldProxy(call)

                call.logger.debug {
                    val path = call.request.path()

                    if (shouldProxyHttp)
                        "Proxying request to backend: $path"
                    else
                        "Processing request: $path"
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
