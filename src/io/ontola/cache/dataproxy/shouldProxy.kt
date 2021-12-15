package io.ontola.cache.dataproxy

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parseHeaderValue
import io.ktor.request.accept
import io.ktor.request.document
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.request.uri

internal fun Configuration.shouldProxy(call: ApplicationCall): Boolean {
    val accept = parseHeaderValue(call.request.accept() ?: "*/*")
        .map { v -> ContentType.parse(v.value).withoutParameters() }
        .toSet()
    val uri = Url(call.request.uri)
    val path = call.request.path()

    val isManifestRequest = call.request.document() == "manifest.json"
    val isPathExcluded = excludedPaths.contains(path)
    val isNotExcluded = !isManifestRequest && !isPathExcluded

    val ext = if (path.contains(".")) path.split(".").lastOrNull() else null
    val isDataReqByExtension = ext?.let { extensions.contains(it) } ?: false
    val isDataReqByAccept = contentTypes.intersect(accept).isNotEmpty()
    val isBinaryRequest = isBinaryRequest(uri, call.request.header(HttpHeaders.Accept))
    val isProxyableComponent = isDataReqByExtension || isDataReqByAccept || isBinaryRequest

    return isNotExcluded && isProxyableComponent
}
