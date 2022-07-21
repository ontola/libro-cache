package tools.empathy.libro.server.dataproxy

import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.content.ChannelWriterContent
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.copyTo
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.util.isDownloadRequest

internal val DataProxyKey = AttributeKey<DataProxy>("DataProxyKey")

private fun Configuration.proxiedUri(
    originalReq: ApplicationRequest,
    call: ApplicationCall
): String = transforms
    .entries
    .find { (path, _) -> path.containsMatchIn(originalReq.uri) }
    ?.let { it.value(call.request) }
    ?: originalReq.uri

suspend fun ApplicationCall.receiveChannelOrNull(): ByteReadChannel? {
    val contentLength = request.headers["Content-Length"]?.toIntOrNull() ?: 0

    return if (contentLength > 0) {
        receiveChannel()
    } else {
        null
    }
}

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
    suspend fun interceptRequest(call: ApplicationCall) {
        val originalReq = call.request
        val uri = Url(originalReq.uri)
        val isDownloadRequest = uri.isDownloadRequest()

        val requestUri = config.proxiedUri(originalReq, call)
        val reqContentType = call.request.headers["Content-Type"]?.let { ContentType.parse(it) }

        val response = config.proxiedRequest(
            call,
            requestUri,
            originalReq.httpMethod,
            call.receiveChannelOrNull()?.let {
                ChannelWriterContent(
                    { it.copyTo(this) },
                    contentType = reqContentType,
                )
            },
        )
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
                    response.content.copyAndClose(channel)
                }
            }
        )
    }
}
