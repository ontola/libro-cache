package tools.empathy.libro.server.dataproxy

import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.util.filter
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.VaryHeader
import java.util.Locale

fun trustedProxyHeaders(
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

    newAuthorizationBulk(response)?.let {
        setAuthorization(
            it.accessToken,
            it.refreshToken,
        )

        it.action?.let { action ->
            set(LibroHttpHeaders.ExecAction, action)
        }
    }

    appendAll(
        proxiedHeaders.filter { key, _ ->
            !config.unsafeList.contains(key.lowercase(Locale.getDefault()))
        }
    )
}
