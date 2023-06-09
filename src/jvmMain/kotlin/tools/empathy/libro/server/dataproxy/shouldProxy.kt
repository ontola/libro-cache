package tools.empathy.libro.server.dataproxy

import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.parseHeaderValue
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.accept
import io.ktor.server.request.document
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import tools.empathy.libro.server.util.isDownloadRequest

/**
 * Determines whether a request should be proxied rather than handled.
 * Exclusion criteria take precedence over inclusion criteria.
 */
internal fun Configuration.shouldProxy(request: ApplicationRequest): Boolean {
    val accept = parseHeaderValue(request.accept() ?: "*/*")
        .map { v -> ContentType.parse(v.value).withoutParameters() }
        .toSet()
    val uri = Url(request.uri)
    val path = request.path()

    val isManifestRequest = request.document() == "manifest.json"
    val isPathExcluded = rules.any { it.exclude && it.match.containsMatchIn(path) }
    val isNotExcluded = !isManifestRequest && !isPathExcluded

    val ext = if (path.contains(".")) path.split(".").lastOrNull() else null
    val isDataReqByExtension = ext?.let { extensions.contains(it) } ?: false
    val isDataReqByAccept = contentTypes.intersect(accept).isNotEmpty()
    val isDataReqByMethod = methods.contains(request.httpMethod)
    val isDownloadRequest = uri.isDownloadRequest()
    val isIncluded = rules.any { !it.exclude && it.match.containsMatchIn(path) }
    val isProxyableComponent = isDataReqByExtension ||
        isDataReqByAccept ||
        isDataReqByMethod ||
        isDownloadRequest ||
        isIncluded

    return isNotExcluded && isProxyableComponent
}
