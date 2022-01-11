package io.ontola.cache.dataproxy

import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.util.filter
import io.ontola.cache.util.VaryHeader
import java.util.Locale

fun fileProxyHeaders(
    config: Configuration,
    isDownloadRequest: Boolean,
    proxiedHeaders: Headers,
    setAuthorization: (String, String) -> Unit,
    response: HttpResponse,
) = Headers.build {
    set(HttpHeaders.Vary, VaryHeader)

    if (isDownloadRequest) {
        append(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.disposition)
    }

    appendAll(
        proxiedHeaders.filter { key, _ ->
            !config.unsafeList.contains(key.lowercase(Locale.getDefault()))
        }
    )
}
